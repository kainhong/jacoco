<!DOCTYPE html>
<html>
<head>
  <title>JaCoCo Code Coverage Report - ${bundleName}</title>
  <link rel="stylesheet" type="text/css" href="jacoco-resources/report.css"/>
  <link rel="shortcut icon" href="jacoco-resources/report.gif" type="image/gif"/>
</head>
<body>
  <span class="absValue">
    <#if footerText?has_content>${footerText}</#if>
  </span>
  <h1>${bundleName}</h1>
  <p style="font-size: large;">Overall Coverage: ${overall_coverage}%</p>
  < E -- TABLE HERE -->
  <table class="coverage" cellspacing="0" cellpadding="0">
    <thead>
      <tr>
        <th>Element</th>
        <th>Covered</th>
        <th>Missed</th>
        <th>Total</th>
        <th>Coverage</th>
      </tr>
    </thead>
    <thead>
      <tr>
        <td class="sortable" id="a" onclick="toggleSort(this)">Element</td>
        <td class="down sortable bar" id="b" onclick="toggleSort(this)">Missed Instructions</td>
        <td class="sortable ctr2" id="c" onclick="toggleSort(this)">Cov.</td>
        <td class="sortable bar" id="d" onclick="toggleSort(this)">Missed Branches</td>
        <td class="sortable ctr2" id="e" onclick="toggleSort(this)">Cov.</td>
        <td class="sortable ctr1" id="f" onclick="toggleSort(this)">Missed</td>
        <td class="sortable ctr1" id="g" onclick="toggleSort(this)">Cxty</td>
        <td class="sortable ctr1" id="h" onclick="toggleSort(this)">Lines</td>
        <td class="sortable ctr1" id="i" onclick="toggleSort(this)">Methods</td>
        <td class="sortable ctr1" id="j" onclick="toggleSort(this)">Classes</td>
      </tr>
    </thead>
    <tbody>
      <#-- Data for overall bundle summary -->
      <#-- Example, adapt based on actual data passed for overall bundle counters -->
      <#--
      <tr>
        <td>Overall</td>
        <td>${bundle.instructionCounter.missedCount}</td>
        <td>${getCoverageBar(bundle.instructionCounter)}</td>
        <td>${bundle.branchCounter.missedCount}</td>
        <td>${getCoverageBar(bundle.branchCounter)}</td>
        <td>... other counters ...</td>
      </tr>
      -->

      <#list coverage_data as item> <#-- This should list packages -->
        <tr class="el_${item.elementType?default('package')}">
          <td><a href="${item.link}">${item.name}</a></td>
          <td class="bar">${item.missedInstructions?c} <span>${item.totalInstructions?c}</span></td>
          <td class="ctr2">${item.instructionCoverage?string["0.00"]}%</td>
          <td class="bar">${item.missedBranches?c} <span>${item.totalBranches?c}</span></td>
          <td class="ctr2">${item.branchCoverage?string["0.00"]}%</td>
          <td class="ctr1">${item.missedComplexity?c}</td>
          <td class="ctr1">${item.totalComplexity?c}</td>
          <td class="ctr1">${item.missedLines?c}</td>
          <td class="ctr1">${item.missedMethods?c}</td>
          <td class="ctr1">${item.missedClasses?c}</td>
        </tr>
      </#list>
    </tbody>
  </table>
  <script type="text/javascript" src="jacoco-resources/sort.js"></script>
</body>
</html>
