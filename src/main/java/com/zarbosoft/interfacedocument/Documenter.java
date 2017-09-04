package com.zarbosoft.interfacedocument;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Documenter {
	public static enum Flavor {
		LUXEM,
		LUA
	}

	public static void document(
			final Reflections reflections,
			final Map<String, String> descriptions,
			final Path out,
			final Flavor flavor,
			final String prefix,
			final Walk.TypeInfo root,
			final List<String> shorten
	) {
		final Map<Type, FluentJSoup.Element> types = new HashMap<>();
		final FluentJSoup.Element toc = FluentJSoup.div();
		final List<String> missingDescriptions = new ArrayList<>();
		final Map<String, String> extraDescriptions = new HashMap<>(descriptions);
		final Function<String, String> getDescription = s -> {
			String d = extraDescriptions.remove(s);
			if (d == null)
				d = descriptions.get(s);
			if (d == null) {
				missingDescriptions.add(s);
				d = "";
			}
			return d;
		};
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
					section.a(a -> a.text("top").withClass("totop").attr("href", "#top"));
					section.a(a -> a.attr("name", enumClass.getTypeName()));
					section.h2(shorten(shorten, enumClass));
					section.p(getDescription.apply(enumClass.getCanonicalName()));
					final FluentJSoup.Element values = FluentJSoup.ul().withClass("definitions");
					section.with(values);
					Walk.enumValues(enumClass).stream().forEach(pair -> {
						final FluentJSoup.Element li =
								FluentJSoup.li().code(code -> code.text(Walk.decideName(pair.second)));
						li.span(getDescription.apply(String.format("%s/%s",
								enumClass.getCanonicalName(),
								pair.second.getName()
						)));
						values.with(li);
					});
					types.put(enumClass, section);
				}
				return FluentJSoup
						.a()
						.attr("href", String.format("#%s", enumClass.getTypeName()))
						.text(shorten(shorten, enumClass));
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
						.text(shorten(shorten, klass));
			}

			public void visitConcrete(
					final Field field, final Class<?> klass, final List<Pair<Field, FluentJSoup.Element>> fields
			) {
				final FluentJSoup.Element section = FluentJSoup.div();
				section.a(a -> a.text("top").withClass("totop").attr("href", "#top"));
				section.a(a -> a.attr("name", klass.getTypeName()));
				section.h2(shorten(shorten, klass));
				section.p(getDescription.apply(klass.getCanonicalName()));
				if (fields.isEmpty()) {
					section.p("This type has no fields.");
				} else {
					final FluentJSoup.Element rows = FluentJSoup.table();
					rows.tr(tr -> tr.withClass("fields").th("Fields").th(""));
					section.with(rows);
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
								final FluentJSoup.Element inner = FluentJSoup.table().withClass("definitions");
								final FluentJSoup.Element cell = FluentJSoup.td();
								cell.p(getDescription.apply(String.format("%s/%s",
										klass.getCanonicalName(),
										f.getName()
								)));
								cell.with(inner);
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
								rows.tr(tr -> tr.td(fieldName).with(cell));
							});
				}
				types.put(klass, section);
			}

			@Override
			public FluentJSoup.Element visitOther(final Field field, final Class<?> otherClass) {
				return FluentJSoup.span().text("");
			}
		});
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
			throw new AssertionError();
		}
		final Stream<FluentJSoup.Element> flavorIntroduction;
		switch (flavor) {
			case LUXEM:
				flavorIntroduction = Stream.of(FluentJSoup.h1().text("Introduction"),
						FluentJSoup
								.p()
								.
										text(String.format("This documentation describes the luxem format for %s.",
												shorten(shorten, root.type)
										))
								.text("  For a description of the syntax, see ")
								.a(a -> a
										.text("the luxem spec homepage")
										.attr("href", "https://github.com/rendaw/luxem"))
								.text("."),
						FluentJSoup.p().text("Note that types only need to be specified where indicated."),
						FluentJSoup
								.p()
								.text("Also, if a type only has one required field, the field can be filled directly, so:"),
						FluentJSoup.code().withClass("block").text("(repeat) { count: 4 }"),
						FluentJSoup.p().text("may be shortened to:"),
						FluentJSoup.code().withClass("block").text("(repeat) 4")
				);
				break;
			case LUA:
				flavorIntroduction = Stream.of(FluentJSoup.h1().text("Introduction"),
						FluentJSoup.p().text(String.format("This documentation describes the Lua configuration for %s.",
								shorten(shorten, root.type)
						)),
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
						toc.a(a -> a
								.text(shorten(shorten, e.getKey()))
								.attr("href", String.format("#%s", e.getKey().getTypeName())));
						body.with(e.getValue());
					});
		}
		try {
			Files.createDirectories(out);
			try (
					OutputStream outStream = Files.newOutputStream(out.resolve("_Sidebar.md"),
							StandardOpenOption.WRITE,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.CREATE
					)
			) {
				outStream.write(toc.render(4).getBytes(StandardCharsets.UTF_8));
			}
			try (
					OutputStream outStream = Files.newOutputStream(out.resolve("Syntax-Reference.md"),
							StandardOpenOption.WRITE,
							StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.CREATE
					)
			) {
				outStream.write(body.render(4).getBytes(StandardCharsets.UTF_8));
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String shorten(final List<String> strings, final Type type) {
		String temp = type.getTypeName();
		for (final String string : strings)
			temp = temp.replaceAll(string, "");
		return temp;
	}
}
