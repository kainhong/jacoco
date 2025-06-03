<!DOCTYPE html>
<html>
<head>
  <title>JaCoCo Code Coverage Report - Sessions</title>
  <link rel="stylesheet" type="text/css" href="jacoco-resources/report.css"/>
  <link rel="shortcut icon" href="jacoco-resources/report.gif" type="image/gif"/>
</head>
<body>
  <span class="absValue">
    <#if footerText?has_content>${footerText}</#if>
  </span>
  <div class="breadcrumb">
    <span class="el_bundle"><a href="${index_link}">Overall</a></span> &gt;
    <span class="el_session">Sessions</span>
  </div>
  <h1>Execution Sessions</h1>
  <table class="coverage" cellspacing="0" cellpadding="0">
    <thead>
      <tr>
        <th>Session ID</th>
        <th>Start Time</th>
        <th>Dump Time</th>
      </tr>
    </thead>
    <tbody>
      <#list sessions as session>
        <tr>
          <td>${session.id}</td>
          <td>${session.startTime}</td>
          <td>${session.dumpTime}</td>
        </tr>
      </#list>
    </tbody>
  </table>
</body>
</html>
