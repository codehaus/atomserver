package org.atomserver.sharding;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

public class DistributionSpecTest {
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;

    @BeforeClass
    public static void init() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(
                "org.atomserver.sharding",
                Distribution.class.getClassLoader());

        marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        unmarshaller = context.createUnmarshaller();
    }

    @Test
    public void testXmlMarshalling() throws Exception {
        Distribution distribution = new Distribution();
        final Selector selector = new Selector();
        selector.setRegex(".*");
        selector.setUrls(Arrays.asList("redis://localhost/1", "redis://localhost/2"));
        distribution.getSelectors().add(selector);

        final StringWriter writer = new StringWriter();
        marshaller.marshal(distribution, writer);
        System.out.println("writer.toString() = " + writer.toString());
    }

    @Test
    public void testDistribution() throws Exception {
        String distributionXml =
                "<sharding:distribution xmlns:sharding=\"http://atomserver.org/sharding\">\n" +
                        "    <selector regex=\"foo/bar/.*\">\n" +
                        "        <url>redis://localhost/9</url>\n" +
                        "    </selector>\n" +
                        "    <selector regex=\"foo/.*\">\n" +
                        "        <url>redis://localhost/5</url>\n" +
                        "        <url>redis://localhost/6</url>\n" +
                        "        <url>redis://localhost/7</url>\n" +
                        "    </selector>\n" +
                        "    <selector regex=\"user:(.*)/.*\">\n" +
                        "        <url>redis://localhost/10</url>\n" +
                        "        <url>redis://localhost/11</url>\n" +
                        "        <url>redis://localhost/12</url>\n" +
                        "    </selector>\n" +
                        "    <selector regex=\"(.*):(.*)/.*\">\n" +
                        "        <url>redis://localhost/13</url>\n" +
                        "        <url>redis://localhost/14</url>\n" +
                        "        <url>redis://localhost/15</url>\n" +
                        "    </selector>\n" +
                        "    <selector regex=\".*\">\n" +
                        "        <url>redis://localhost/1</url>\n" +
                        "        <url>redis://localhost/2</url>\n" +
                        "    </selector>\n" +
                        "</sharding:distribution>";
        Distribution distribution = (Distribution) unmarshaller.unmarshal(new StringReader(distributionXml));
        Distribution.CompiledDistribution compiledDistribution = distribution.compile();
        Assert.assertEquals("redis://localhost/9", compiledDistribution.map("foo/bar/baz"));
        Assert.assertEquals("redis://localhost/9", compiledDistribution.map("foo/bar/"));
        Assert.assertEquals("redis://localhost/5", compiledDistribution.map("foo/a"));
        Assert.assertEquals("redis://localhost/6", compiledDistribution.map("foo/b"));
        Assert.assertEquals("redis://localhost/2", compiledDistribution.map("fu/bar"));
        Assert.assertEquals("redis://localhost/1", compiledDistribution.map("fu/bam"));

        Assert.assertEquals("redis://localhost/12", compiledDistribution.map("user:bryon/a"));
        Assert.assertEquals("redis://localhost/12", compiledDistribution.map("user:bryon/b"));
        Assert.assertEquals("redis://localhost/12", compiledDistribution.map("user:bryon/c"));
        Assert.assertEquals("redis://localhost/10", compiledDistribution.map("user:alex/a"));
        Assert.assertEquals("redis://localhost/10", compiledDistribution.map("user:alex/b"));
        Assert.assertEquals("redis://localhost/10", compiledDistribution.map("user:alex/c"));

        Assert.assertEquals("redis://localhost/15", compiledDistribution.map("x:bryon/a"));
        Assert.assertEquals("redis://localhost/15", compiledDistribution.map("x:bryon/b"));
        Assert.assertEquals("redis://localhost/15", compiledDistribution.map("x:bryon/c"));
        Assert.assertEquals("redis://localhost/14", compiledDistribution.map("y:bryon/a"));
        Assert.assertEquals("redis://localhost/14", compiledDistribution.map("y:bryon/b"));
        Assert.assertEquals("redis://localhost/14", compiledDistribution.map("y:bryon/c"));
        Assert.assertEquals("redis://localhost/13", compiledDistribution.map("x:alex/a"));
        Assert.assertEquals("redis://localhost/13", compiledDistribution.map("x:alex/b"));
        Assert.assertEquals("redis://localhost/13", compiledDistribution.map("x:alex/c"));
    }
}
