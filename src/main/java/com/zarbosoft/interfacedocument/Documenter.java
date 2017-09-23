package com.zarbosoft.interfacedocument;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.rendaw.common.Pair;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Documenter {
	public static enum Flavor {
		LUXEM,
		LUA
	}

	public static boolean document(
			final Reflections reflections,
			final Map<String, String> descriptions,
			final Path out,
			final Flavor flavor,
			final String prefix,
			final Walk.TypeInfo root
	) {
		final Map<Type, FluentJSoup.Element> types = new HashMap<>();
		final FluentJSoup.Element toc = FluentJSoup.div();
		final List<String> missingDescriptions = new ArrayList<>();
		final Map<String, String> extraDescriptions = new HashMap<>(descriptions);
		final Function<String, Stream<FluentJSoup.Node>> getDescription = s -> {
			String d = extraDescriptions.remove(s);
			if (d == null)
				d = descriptions.get(s);
			if (d == null) {
				missingDescriptions.add(s);
				d = "";
			}
			return transformText(d);
		};

		final Map<Type, List<FluentJSoup.Node>> shortNodes = new HashMap<>();
		final Map<String, Integer> shortCounts = new HashMap<>();
		final Function<Type, FluentJSoup.Node> shorten = type -> {
			final List<FluentJSoup.Node> list = shortNodes.computeIfAbsent(type, t -> new ArrayList<>());
			if (list.isEmpty()) {
				final List<String> splits = Arrays.asList(type.getTypeName().split("\\."));
				for (int i = 1; i < splits.size(); ++i) {
					final String key =
							splits.subList(splits.size() - i, splits.size()).stream().collect(Collectors.joining("."));
					shortCounts.compute(key, (k2, count) -> (count == null ? 0 : count) + 1);
				}
			}
			final FluentJSoup.Node o = FluentJSoup.text("");
			list.add(o);
			return o;
		};

		// Build body and toc
		final FluentJSoup.Element rootElement = Walk.walk(reflections, root, new Walk.Visitor<FluentJSoup.Element>() {
			public FluentJSoup.Element visitString(final Field field) {
				return FluentJSoup.span().text("Any string");
			}

			public FluentJSoup.Element visitInteger(final Field field) {
				return FluentJSoup.span().text("Any integer");
			}

			public FluentJSoup.Element visitDouble(final Field field) {
				return FluentJSoup.span().text("Any decimal value");
			}

			public FluentJSoup.Element visitBoolean(final Field field) {
				return FluentJSoup.ul().li(li -> li.code("true")).li(li -> li.code("false"));
			}

			public FluentJSoup.Element visitEnum(final Field field, final Class<?> enumClass) {
				if (!types.containsKey(enumClass)) {
					final FluentJSoup.Element section = FluentJSoup.div();
					section.a(a -> a.attr("name", enumClass.getTypeName()));
					section.h2(h2 -> h2.with(shorten.apply(enumClass)));
					section.p(p -> p.with(getDescription.apply(enumClass.getCanonicalName())));
					final FluentJSoup.Element values = FluentJSoup.ul();
					section.with(values);
					Walk.enumValues(enumClass).stream().forEach(pair -> {
						final FluentJSoup.Element li =
								FluentJSoup.li().code(code -> code.text(Walk.decideName(pair.second)));
						li.span(span -> span.with(getDescription.apply(String.format("%s/%s",
								enumClass.getCanonicalName(),
								pair.second.getName()
						))));
						values.with(li);
					});
					types.put(enumClass, section);
				}
				return FluentJSoup
						.a()
						.attr("href", String.format("#%s", enumClass.getTypeName()))
						.with(shorten.apply(enumClass));
			}

			public FluentJSoup.Element visitList(final Field field, final FluentJSoup.Element inner) {
				return FluentJSoup.span().p("List of:").with(inner);
			}

			public FluentJSoup.Element visitSet(final Field field, final FluentJSoup.Element inner) {
				return FluentJSoup.span().p("Set of:").with(inner);
			}

			public FluentJSoup.Element visitMap(final Field field, final FluentJSoup.Element inner) {
				return FluentJSoup.span().p("Nested:").with(inner);
			}

			public FluentJSoup.Element visitAbstract(
					final Field field, final Class<?> klass, final List<Pair<Class<?>, FluentJSoup.Element>> derived
			) {
				final FluentJSoup.Element inner = FluentJSoup.ul();
				derived.forEach(pair -> {
					final String name = Walk.decideName(pair.first);
					inner.li(li -> li.span(String.format("(%s) ", name)).with(pair.second));
				});
				return FluentJSoup.span().p("Any of (specify type):").with(inner);
			}

			public FluentJSoup.Element visitConcreteShort(final Field field, final Class<?> klass) {
				return FluentJSoup
						.a()
						.attr("href", String.format("#%s", klass.getTypeName()))
						.with(shorten.apply(klass));
			}

			public void visitConcrete(
					final Field field, final Class<?> klass, final List<Pair<Field, FluentJSoup.Element>> fields
			) {
				final FluentJSoup.Element section = FluentJSoup.div();
				section.a(a -> a.attr("name", klass.getTypeName()));
				section.h2(h2 -> h2.with(shorten.apply(klass)));
				section.p(p -> p.with(getDescription.apply(klass.getCanonicalName())));
				if (fields.isEmpty()) {
					section.p("This type has no fields.");
				} else {
					final Common.Mutable<Object> instance = new Common.Mutable<>();
					fields
							.stream()
							.sorted(new ChainComparator<Pair<Field, FluentJSoup.Element>>()
									.trueFirst(p -> Walk.required(p.first))
									.lesserFirst(p -> Walk.decideName(p.first))
									.build())
							.forEach(pair -> {
								final Field f = pair.first;
								final String fieldName = Walk.decideName(f);
								section.h4(String.format("field: %s", fieldName));
								section.p(p -> p.with(getDescription.apply(String.format("%s/%s",
										klass.getCanonicalName(),
										f.getName()
								))));
								final FluentJSoup.Element inner = FluentJSoup.table();
								inner.tr(tr -> tr.td("Values").td(td -> td.with(pair.second)));
								inner.tr(tr -> tr.td("Required").
										td(td -> {
											if (Walk.required(f))
												td.b("yes");
											else
												td.span("no");
										}));
								if (!Walk.required(f) && (
										f.getType() == String.class ||
												f.getType() == int.class ||
												f.getType() == Integer.class ||
												f.getType() == double.class ||
												f.getType() == Double.class ||
												f.getType() == boolean.class ||
												f.getType() == Boolean.class ||
												((Class<?>) f.getType()).isEnum()
								)) {
									if (instance.value == null) {
										instance.value = uncheck(() -> klass.getConstructor().newInstance());
									}
									final Object defaultValue = uncheck(() -> f.get(instance.value));
									if (defaultValue != null) {
										final FluentJSoup.Element row = FluentJSoup.tr();
										row.td("Default value");
										if (((Class<?>) f.getType()).isEnum()) {
											row.td(td -> td.code(Walk.decideEnumName((Enum<?>) defaultValue)));
										} else {
											final String defaultString;
											try {
												defaultString = defaultValue.toString();
											} catch (final Exception e) {
												throw new AssertionError(String.format(
														"Error formatting string for default value of field [%s] in %s.",
														f.getName(),
														klass.getCanonicalName()
												), e);
											}
											row.td(td -> td.code(String.format("%s", defaultString)));
										}
										inner.with(row);
									}
								}
								section.with(inner);
							});
				}
				types.put(klass, section);
			}

			@Override
			public FluentJSoup.Element visitOther(final Field field, final Class<?> otherClass) {
				return FluentJSoup.span().text("");
			}
		});
		final boolean success;
		if (!missingDescriptions.isEmpty() || !extraDescriptions.isEmpty()) {
			System.out.format("\n\nMISSING\n");
			for (final String error : missingDescriptions) {
				System.out.format("    %s: \"\",\n", error);
			}
			System.out.format("\n\nEXTRA\n");
			for (final Map.Entry<String, String> entry : extraDescriptions.entrySet()) {
				System.out.format("%s\n", entry.getKey());
			}
			System.out.flush();
			success = false;
		} else
			success = true;

		// Build intro
		final Stream<FluentJSoup.Node> flavorIntroduction;
		switch (flavor) {
			case LUXEM:
				flavorIntroduction = Stream.of(FluentJSoup.h1().text("Introduction"),
						FluentJSoup
								.p()
								.text("This documentation describes the luxem format for ")
								.code(code -> code.with(shorten.apply(root.type)))
								.text(".")
								.text("  For a description of the syntax, see ")
								.a(a -> a
										.text("the luxem spec homepage")
										.attr("href", "https://github.com/rendaw/luxem"))
								.text("."),
						FluentJSoup.p().text("Note that types only need to be specified where indicated."),
						FluentJSoup
								.p()
								.text("Also, if a type only has one required field, the field can be filled directly, so:"),
						FluentJSoup.code().text("(repeat) { count: 4 }"),
						FluentJSoup.p().text("may be shortened to:"),
						FluentJSoup.code().text("(repeat) 4")
				);
				break;
			case LUA:
				flavorIntroduction = Stream.of(FluentJSoup.h1().text("Introduction"),
						FluentJSoup
								.p()
								.text("This documentation describes the Lua configuration for ")
								.code(code -> code.with(shorten.apply(root.type)))
								.text("."),
						FluentJSoup
								.p()
								.text("Some values are typed.  Specify types with built-in unary functions which take the value to " +
										"be typed.  For example, to place a dog in field animal, write ")
								.code("dog { name = \"fido\" }")
								.text(".  Types are indicated in the documentation as a word in parentheses."),
						FluentJSoup.p().text("Note that types only need to be specified where indicated."),
						FluentJSoup
								.p()
								.text("Also, if a type only has one required field, the field can be filled directly, so:"),
						FluentJSoup.pre().code("repeat { count = 4 }"),
						FluentJSoup.p().text("may be shortened to:"),
						FluentJSoup.pre().code("repeat 4")
				);
				break;
			default:
				throw new DeadCode();
		}

		// Assemble body
		final FluentJSoup.Element body = FluentJSoup
				.div()
				.with(flavorIntroduction)
				.h2("Document Root")
				.p("The root element of the document is:")
				.with(rootElement);
		if (!types.isEmpty()) {
			body.h1("Types");
			types
					.entrySet()
					.stream()
					.sorted(new ChainComparator<Map.Entry<Type, FluentJSoup.Element>>()
							.lesserFirst(o -> o.getKey().getTypeName())
							.build())
					.forEach(e -> {
						toc.div(div -> div.a(a -> a
								.with(shorten.apply(e.getKey()))
								.attr("href", String.format("#%s", e.getKey().getTypeName()))));
						body.with(e.getValue());
						body.br();
						body.br();
					});
		}

		// Resolve short names
		for (final Map.Entry<Type, List<FluentJSoup.Node>> type : shortNodes.entrySet()) {
			final List<String> splits = Arrays.asList(type.getKey().getTypeName().split("\\."));
			boolean set = false;
			for (int i = 1; i < splits.size(); ++i) {
				final String key =
						splits.subList(splits.size() - i, splits.size()).stream().collect(Collectors.joining("."));
				if (shortCounts.get(key) > 1)
					continue;
				type.getValue().forEach(v -> v.setText(key));
				set = true;
				break;
			}
			if (!set)
				type.getValue().forEach(v -> v.setText(type.getKey().getTypeName()));
		}

		try {
			Files.createDirectories(out);
			try (
					OutputStream outStream = Files.newOutputStream(out.resolve("_Sidebar.rst"),
							StandardOpenOption.WRITE,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.CREATE
					)
			) {
				Documenter.writeRst(outStream, toc);
			}
			try (
					OutputStream outStream = Files.newOutputStream(out.resolve("Syntax-Reference.rst"),
							StandardOpenOption.WRITE,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.CREATE
					)
			) {
				Documenter.writeRst(outStream, body);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		return success;
	}

	public static Stream<FluentJSoup.Node> transformText(final String text) {
		final String[] codes = text.split("`");
		final ImmutableList.Builder<FluentJSoup.Node> builder = ImmutableList.builder();
		for (int i = 0; i < codes.length; ++i) {
			if (i % 2 == 0) {
				final String[] lines = codes[i].split("\n");
				for (int j = 0; j < lines.length; ++j) {
					if (j > 0)
						builder.add(FluentJSoup.br());
					builder.add(FluentJSoup.text(lines[j]));
				}
			} else {
				builder.add(FluentJSoup.code().text(codes[i]));
			}
		}
		return builder.build().stream();
	}

	public static void writeRst(final OutputStream out, final FluentJSoup.Element body) {
		uncheck(() -> {
			out.write(".. raw:: html\n\n".getBytes(StandardCharsets.UTF_8));
			out.write(Pattern
					.compile("^", Pattern.MULTILINE)
					.matcher(body.render(4))
					.replaceAll("    ")
					.getBytes(StandardCharsets.UTF_8));
		});
	}
}
