<xsl:stylesheet xmlns:jakartaee="https://jakarta.ee/xml/ns/jakartaee" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes" />
    <xsl:strip-space elements="*" />

    <xsl:template match="node()|@*" name="identity">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="jakartaee:facelet-taglib/jakartaee:description/text()">
        This is a copy of omnifaces-ui.taglib.xml with old namespace for sake of backwards compatibility.
        This will be removed in a future OmniFaces version.
        So, you need to migrate XML namespace from xmlns:o="http://omnifaces.org/ui" to xmlns:o="omnifaces.ui" namespace as soon as possible.
    </xsl:template>

    <xsl:template match="jakartaee:namespace/text()">http://omnifaces.org/ui</xsl:template>

    <xsl:template match="jakartaee:facelet-taglib/jakartaee:tag/jakartaee:description/text()">
        Please migrate XML namespace from xmlns:o="http://omnifaces.org/ui" to xmlns:o="omnifaces.ui" as soon as possible.
    </xsl:template>

    <xsl:template match="jakartaee:facelet-taglib/jakartaee:tag/jakartaee:attribute/jakartaee:description" />
</xsl:stylesheet>