package com.zarbosoft.interfacedocument;

import org.jsoup.nodes.Document;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class FluentJSoup {
	public static class Element {

		private final org.jsoup.nodes.Element element;

		private Element(final org.jsoup.nodes.Element element) {
			this.element = element;
		}

		public String render(final int indent) {
			final Document document = element instanceof Document ? (Document) element : new Document("");
			document.outputSettings().prettyPrint(true).indentAmount(indent);
			document.appendChild(element);
			return element.html();
		}

		public String render() {
			final Document document = element instanceof Document ? (Document) element : new Document("");
			document.appendChild(element);
			return element.html();
		}

		public Element text(final String text) {
			element.appendText(text);
			return this;
		}

		public Element attr(final String name, final String value) {
			element.attr(name, value);
			return this;
		}

		public Element withClass(final String name) {
			element.addClass(name);
			return this;
		}

		public Element with(final String name, final String text) {
			element.appendElement(name).text(text);
			return this;
		}

		public Element with(final String name, final Consumer<Element> inner) {
			inner.accept(new Element(element.appendElement(name)));
			return this;
		}

		public Element with(final Element element) {
			this.element.appendChild(element.element);
			return this;
		}

		public Element with(final Stream<Element> elements) {
			elements.forEach(e -> Element.this.with(e));
			return this;
		}

		public Element head(final String text) {
			return with("head", text);
		}

		public Element head(final Consumer<Element> inner) {
			return with("head", inner);
		}

		public Element meta(final String text) {
			return with("meta", text);
		}

		public Element meta(final Consumer<Element> inner) {
			return with("meta", inner);
		}

		public Element title(final String text) {
			return with("title", text);
		}

		public Element title(final Consumer<Element> inner) {
			return with("title", inner);
		}

		public Element link(final String text) {
			return with("link", text);
		}

		public Element link(final Consumer<Element> inner) {
			return with("link", inner);
		}

		public Element body(final String text) {
			return with("body", text);
		}

		public Element body(final Consumer<Element> inner) {
			return with("body", inner);
		}

		public Element p(final String text) {
			return with("p", text);
		}

		public Element p(final Consumer<Element> inner) {
			return with("p", inner);
		}

		public Element b(final String text) {
			return with("b", text);
		}

		public Element b(final Consumer<Element> inner) {
			return with("b", inner);
		}

		public Element div(final String text) {
			return with("div", text);
		}

		public Element div(final Consumer<Element> inner) {
			return with("div", inner);
		}

		public Element span(final String text) {
			return with("span", text);
		}

		public Element span(final Consumer<Element> inner) {
			return with("span", inner);
		}

		public Element ul(final String text) {
			return with("ul", text);
		}

		public Element ul(final Consumer<Element> inner) {
			return with("ul", inner);
		}

		public Element li(final String text) {
			return with("li", text);
		}

		public Element li(final Consumer<Element> inner) {
			return with("li", inner);
		}

		public Element br(final String text) {
			return with("br", text);
		}

		public Element br(final Consumer<Element> inner) {
			return with("br", inner);
		}

		public Element a(final String text) {
			return with("a", text);
		}

		public Element a(final Consumer<Element> inner) {
			return with("a", inner);
		}

		public Element h1(final String text) {
			return with("h1", text);
		}

		public Element h1(final Consumer<Element> inner) {
			return with("h1", inner);
		}

		public Element h2(final String text) {
			return with("h2", text);
		}

		public Element h2(final Consumer<Element> inner) {
			return with("h2", inner);
		}

		public Element h3(final String text) {
			return with("h3", text);
		}

		public Element h3(final Consumer<Element> inner) {
			return with("h3", inner);
		}

		public Element h4(final String text) {
			return with("h4", text);
		}

		public Element h4(final Consumer<Element> inner) {
			return with("h4", inner);
		}

		public Element h5(final String text) {
			return with("h5", text);
		}

		public Element h5(final Consumer<Element> inner) {
			return with("h5", inner);
		}

		public Element h6(final String text) {
			return with("h6", text);
		}

		public Element h6(final Consumer<Element> inner) {
			return with("h6", inner);
		}

		public Element code(final String text) {
			return with("code", text);
		}

		public Element code(final Consumer<Element> inner) {
			return with("code", inner);
		}

		public Element pre(final String text) {
			return with("pre", text);
		}

		public Element pre(final Consumer<Element> inner) {
			return with("pre", inner);
		}

		public Element table(final String text) {
			return with("table", text);
		}

		public Element table(final Consumer<Element> inner) {
			return with("table", inner);
		}

		public Element th(final String text) {
			return with("th", text);
		}

		public Element th(final Consumer<Element> inner) {
			return with("th", inner);
		}

		public Element tr(final String text) {
			return with("tr", text);
		}

		public Element tr(final Consumer<Element> inner) {
			return with("tr", inner);
		}

		public Element td(final String text) {
			return with("td", text);
		}

		public Element td(final Consumer<Element> inner) {
			return with("td", inner);
		}
	}

	public static Element html() {
		return new Element(new org.jsoup.nodes.Document(null));
	}

	public static Element head() {
		return new Element(new org.jsoup.nodes.Element("head"));
	}

	public static Element body() {
		return new Element(new org.jsoup.nodes.Element("body"));
	}

	public static Element p() {
		return new Element(new org.jsoup.nodes.Element("p"));
	}

	public static Element a() {
		return new Element(new org.jsoup.nodes.Element("a"));
	}

	public static Element b() {
		return new Element(new org.jsoup.nodes.Element("b"));
	}

	public static Element span() {
		return new Element(new org.jsoup.nodes.Element("span"));
	}

	public static Element div() {
		return new Element(new org.jsoup.nodes.Element("div"));
	}

	public static Element br() {
		return new Element(new org.jsoup.nodes.Element("br"));
	}

	public static Element ul() {
		return new Element(new org.jsoup.nodes.Element("ul"));
	}

	public static Element li() {
		return new Element(new org.jsoup.nodes.Element("li"));
	}

	public static Element h1() {
		return new Element(new org.jsoup.nodes.Element("h1"));
	}

	public static Element h2() {
		return new Element(new org.jsoup.nodes.Element("h2"));
	}

	public static Element h3() {
		return new Element(new org.jsoup.nodes.Element("h3"));
	}

	public static Element h4() {
		return new Element(new org.jsoup.nodes.Element("h4"));
	}

	public static Element h5() {
		return new Element(new org.jsoup.nodes.Element("h5"));
	}

	public static Element h6() {
		return new Element(new org.jsoup.nodes.Element("h6"));
	}

	public static Element table() {
		return new Element(new org.jsoup.nodes.Element("table"));
	}

	public static Element th() {
		return new Element(new org.jsoup.nodes.Element("th"));
	}

	public static Element tr() {
		return new Element(new org.jsoup.nodes.Element("tr"));
	}

	public static Element td() {
		return new Element(new org.jsoup.nodes.Element("td"));
	}

	public static Element code() {
		return new Element(new org.jsoup.nodes.Element("code"));
	}

	public static Element pre() {
		return new Element(new org.jsoup.nodes.Element("pre"));
	}
}
