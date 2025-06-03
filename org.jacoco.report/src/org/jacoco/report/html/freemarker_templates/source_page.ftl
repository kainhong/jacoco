<!DOCTYPE html>
<html>
<head>
  <title>JaCoCo Code Coverage Report - ${file_name}</title>
  <link rel="stylesheet" type="text/css" href="../../jacoco-resources/report.css"/> <#-- Relative path, 2 levels up -->
  <link rel="stylesheet" type="text/css" href="../../jacoco-resources/prettify.css"/>
  <link rel="shortcut icon" href="../../jacoco-resources/report.gif" type="image/gif"/>
</head>
<body onload="window['PR_TAB_WIDTH']=4;prettyPrint()">
  <span class="absValue">
    <#if footerText?has_content>${footerText}</#if>
  </span>
  <div class="breadcrumb">
    <span class="el_bundle"><a href="../../index.html">Overall</a></span> &gt;
    <span class="el_package"><a href="${group_link}">${group_name}</a></span> &gt;
    <span class="el_source">${file_name}</span>
  </div>
  <h1>${file_name}</h1>
  <pre class="source lang-java linenums">
<#list source_lines as line>
  <span class=" κάθε γραμμή ${line.css_class?default('pln')}">${line.text?html}</span>
</#list>
  </pre>
  <script type="text/javascript" src="../../jacoco-resources/prettify.js"></script>
</body>
</html>
