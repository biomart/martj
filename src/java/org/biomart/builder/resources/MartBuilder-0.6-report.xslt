<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="UTF-8" omit-xml-declaration="yes" standalone="no" indent="no" media-type="text/plain"/>

<xsl:key name="ids" match="//*" use="@id"/>

<xsl:template match="/">
 <xsl:apply-templates select="/mart"/>
</xsl:template>

<xsl:template name="idsToNames">
  <xsl:param name="str"/>
  <xsl:choose>
    <xsl:when test="contains($str,',')">
      <xsl:value-of select="key('ids',substring-before($str,','))/@name"/>, 
      <xsl:call-template name="idsToNames">
        <xsl:with-param name="str" select="substring-after($str,',')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="key('ids',$str)/@name"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="twoColumnPrintout">
  <xsl:param name="str1"/>
  <xsl:param name="str2"/>
  <xsl:choose>
    <xsl:when test="contains($str1,',')">
      <xsl:value-of select="substring-before($str1,',')"/> => <xsl:value-of select="substring-before($str2,',')"/>
<xsl:text>
</xsl:text>
      <xsl:call-template name="twoColumnPrintout">
        <xsl:with-param name="str1" select="substring-after($str1,',')"/>
        <xsl:with-param name="str2" select="substring-after($str2,',')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$str1"/> => <xsl:value-of select="$str2"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<!-- MART -->
<xsl:template match="/mart">
Mart report
===========

Default target schema for SQL output: <xsl:choose><xsl:when test="@outputSchema"><xsl:value-of select="@outputSchema"/></xsl:when><xsl:otherwise>-- not specified --</xsl:otherwise></xsl:choose>
Default host/port for remote host output: <xsl:choose><xsl:when test="@outputHost"><xsl:value-of select="@outputHost"/>/<xsl:value-of select="@outputPort"/></xsl:when><xsl:otherwise>-- not specified --</xsl:otherwise></xsl:choose>

Schemas
=======
<xsl:apply-templates select="./jdbcSchema"/> 

Inter-schema (external) relations (if any)
==========================================
<xsl:apply-templates select="./relation"/> 

Datasets
========
<xsl:apply-templates select="./dataset"/> 
</xsl:template>


<!-- JDBC SCHEMA -->
<xsl:template match="/mart/jdbcSchema">
JDBC Schema
+++++++++++

Name: <xsl:value-of select="@name"/>

Driver class: <xsl:value-of select="@driverClassName"/>
Driver location: <xsl:value-of select="@driverClassLocation"/>
URL: <xsl:value-of select="@url"/>
Database schema: <xsl:value-of select="@schemaName"/>
Username: <xsl:value-of select="@username"/>
Password: <xsl:value-of select="@password"/>

Uses keyguessing?: <xsl:value-of select="@keyguessing"/>
Schema partitions: 
<xsl:choose><xsl:when test="@partitionSchemas"><xsl:call-template name="twoColumnPrintout"><xsl:with-param name="str1" select="@partitionSchemas"/><xsl:with-param name="str2" select="@partitionPrefixes"/></xsl:call-template></xsl:when><xsl:otherwise>-- none specified --</xsl:otherwise></xsl:choose>

Incorrect keys and relations (if any)
-------------------------------------
<xsl:apply-templates select="./table/primaryKey[@status='HANDMADE']"/> 
<xsl:apply-templates select="./table/foreignKey[@status='HANDMADE']"/> 
<xsl:apply-templates select="./relation[@status='HANDMADE']"/> 

Additional/modified keys and relations (if any)
-----------------------------------------------
<xsl:apply-templates select="./table/primaryKey[@status='INFERRED_INCORRECT']"/> 
<xsl:apply-templates select="./table/foreignKey[@status='INFERRED_INCORRECT']"/> 
<xsl:apply-templates select="./relation[@status='INFERRED_INCORRECT']"/> 
</xsl:template>


<!-- KEYS -->
<xsl:template match="foreignKey">
 <xsl:call-template name="key"/>
</xsl:template>
<xsl:template match="primaryKey">
 <xsl:call-template name="key"/>
</xsl:template>
<xsl:template name="key">
Key: 
  Table: <xsl:value-of select="../@name"/> 
  Columns: [<xsl:call-template name="idsToNames"><xsl:with-param name="str" select="@columnIds"/></xsl:call-template>]
</xsl:template>


<!-- RELATIONS -->
<xsl:template match="relation">
Relation:
         Type: <xsl:value-of select="@cardinality"/>
         From: <xsl:apply-templates select="key('ids',@firstKeyId)"/>           To: <xsl:apply-templates select="key('ids',@secondKeyId)"/>
</xsl:template>


<!-- DATASETS -->
<xsl:template match="/mart/dataset">
Dataset
+++++++

Name: <xsl:value-of select="@name"/>
Focused on: 
  Schema: <xsl:apply-templates select="key('ids',@centralTableId)/../@name"/> 
   Table: <xsl:call-template name="idsToNames"><xsl:with-param name="str" select="@centralTableId"/></xsl:call-template>
Optimiser type: <xsl:value-of select="@optimiser"/>
Optimisers indexed?: <xsl:value-of select="@indexOptimiser"/>
Subclass optimisers?: <xsl:value-of select="@subclassOptimiser"/>
Invisible?: <xsl:value-of select="@invisible"/>

Dataset-wide modifications (if-any)
------------------------------------
<xsl:apply-templates select="./*[not(@tableKey) or @tableKey='__DATASET__WIDE__']"/>

Table-level modifications (if any)
----------------------------------
<xsl:apply-templates select="./*[@tableKey and not(@tableKey='__DATASET__WIDE__')]"/>

</xsl:template>


<!-- DATASET AND SCHEMA MODS -->

<xsl:template match="subclassRelation">

Subclass relation: <xsl:apply-templates select="key('ids',@relationId)"/>
</xsl:template>

<xsl:template match="compoundRelation">

Compound relation: <xsl:apply-templates select="key('ids',@relationId)"/>
<xsl:if test="not(@tableKey='__DATASET__WIDE__')">Applies only to dataset table: <xsl:value-of select="@tableKey"/></xsl:if>
Number of times compounded (arity): <xsl:value-of select="@n"/>
Compound in parallel?: <xsl:value-of select="@parallel"/>
</xsl:template>

<xsl:template match="compoundRelation">

Directional relation: <xsl:apply-templates select="key('ids',@relationId)"/>
<xsl:if test="not(@tableKey='__DATASET__WIDE__')">Applies only to dataset table: <xsl:value-of select="@tableKey"/></xsl:if>
Starting key: <xsl:apply-templates select="key('ids',@keyId)"/>
</xsl:template>

<xsl:template match="renamedTable">

Table renamed from <xsl:value-of select="@tableKey"/> to <xsl:value-of select="@newName"/>
</xsl:template>

<xsl:template match="renamedColumn">

Renamed column from <xsl:value-of select="@colKey"/> to <xsl:value-of select="@newName"/> on table <xsl:value-of select="@tableKey"/> 
</xsl:template>

<xsl:template match="nonInheritedColumn">

Non-inherited column <xsl:value-of select="@colKey"/> on table <xsl:value-of select="@tableKey"/>
</xsl:template>

<xsl:template match="maskedColumn">

Masked column <xsl:value-of select="@colKey"/> on table <xsl:value-of select="@tableKey"/>
</xsl:template>

<xsl:template match="indexedColumn">

Indexed column <xsl:value-of select="@colKey"/> on table <xsl:value-of select="@tableKey"/>
</xsl:template>

<xsl:template match="maskedRelation">

Masked relation: <xsl:apply-templates select="key('ids',@relationId)"/>
<xsl:if test="not(@tableKey='__DATASET__WIDE__')">Applies only to dataset table: <xsl:value-of select="@tableKey"/></xsl:if>
</xsl:template>

<xsl:template match="maskedTable">

Masked table: <xsl:value-of select="@tableKey"/>
</xsl:template>

<xsl:template match="mergedRelation">

Merged relation: <xsl:apply-templates select="key('ids',@relationId)"/>
</xsl:template>

<xsl:template match="forcedRelation">

Forced relation: <xsl:apply-templates select="key('ids',@relationId)"/>
<xsl:if test="not(@tableKey='__DATASET__WIDE__')">Applies only to dataset table: <xsl:value-of select="@tableKey"/></xsl:if>
</xsl:template>

<xsl:template match="partitionedColumn">

Partitioned column <xsl:value-of select="@colKey"/> on table <xsl:value-of select="@tableKey"/>
Type: <xsl:value-of select="@partitionType"/>
<xsl:choose>
<xsl:when test="@partitionType='valueList'">
Value list:
<xsl:call-template name="twoColumnPrintout"><xsl:with-param name="str1" select="@valueValues"/><xsl:with-param name="str2" select="@valueNames"/></xsl:call-template>
</xsl:when>
<xsl:when test="@partitionType='valueRange'">
Range list:
<xsl:call-template name="twoColumnPrintout"><xsl:with-param name="str1" select="@rangeExpressions"/><xsl:with-param name="str2" select="@rangeNames"/></xsl:call-template>
</xsl:when>
<xsl:otherwise>
-- unknown --
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<!--
<!ELEMENT restrictedTable EMPTY>
<!ATTLIST restrictedTable tableKey CDATA #REQUIRED>
<!ATTLIST restrictedTable tableId IDREF #REQUIRED>
<!ATTLIST restrictedTable aliasRelationIds IDREFS #REQUIRED>
<!ATTLIST restrictedTable aliasColumnIds IDREFS #REQUIRED>
<!ATTLIST restrictedTable aliasNames CDATA #REQUIRED>
<!ATTLIST restrictedTable expression CDATA #REQUIRED>
<!ATTLIST restrictedTable hard (true|false) #REQUIRED>

<!ELEMENT restrictedRelation EMPTY>
<!ATTLIST restrictedRelation tableKey CDATA #REQUIRED>
<!ATTLIST restrictedRelation relationId IDREF #REQUIRED>
<!ATTLIST restrictedRelation index CDATA #REQUIRED>
<!ATTLIST restrictedRelation leftAliasColumnIds IDREFS #REQUIRED>
<!ATTLIST restrictedRelation leftAliasNames CDATA #REQUIRED>
<!ATTLIST restrictedRelation rightAliasColumnIds IDREFS #REQUIRED>
<!ATTLIST restrictedRelation rightAliasNames CDATA #REQUIRED>
<!ATTLIST restrictedRelation expression CDATA #REQUIRED>
<!ATTLIST restrictedRelation hard (true|false) #IMPLIED>

<!ELEMENT expressionColumn EMPTY>
<!ATTLIST expressionColumn tableKey CDATA #REQUIRED>
<!ATTLIST expressionColumn colKey CDATA #REQUIRED>
<!ATTLIST expressionColumn aliasColumnNames CDATA #REQUIRED>
<!ATTLIST expressionColumn aliasNames CDATA #REQUIRED>
<!ATTLIST expressionColumn expression CDATA #REQUIRED>
<!ATTLIST expressionColumn groupBy  (true|false) #REQUIRED>
<!ATTLIST expressionColumn optimiser  (true|false) #REQUIRED>

<!ELEMENT concatRelation EMPTY>
<!ATTLIST concatRelation tableKey CDATA #REQUIRED>
<!ATTLIST concatRelation colKey CDATA #REQUIRED>
<!ATTLIST concatRelation relationId IDREF #REQUIRED>
<!ATTLIST concatRelation index CDATA #REQUIRED>
<!ATTLIST concatRelation aliasRelationIds IDREFS #REQUIRED>
<!ATTLIST concatRelation aliasColumnIds IDREFS #REQUIRED>
<!ATTLIST concatRelation aliasNames CDATA #REQUIRED>
<!ATTLIST concatRelation expression CDATA #REQUIRED>
<!ATTLIST concatRelation rowSep CDATA #REQUIRED>
<!ATTLIST concatRelation recursionType (NONE|PREPEND|APPEND) #REQUIRED>
<!ATTLIST concatRelation recursionKey IDREF #IMPLIED>
<!ATTLIST concatRelation firstRelation IDREF #IMPLIED>
<!ATTLIST concatRelation secondRelation IDREF #IMPLIED>
<!ATTLIST concatRelation concSep CDATA #IMPLIED>
-->

</xsl:stylesheet>