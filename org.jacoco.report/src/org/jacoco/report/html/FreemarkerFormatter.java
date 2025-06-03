package org.jacoco.report.html;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.report.IHTMLReportOutput; // Keep if createVisitor is kept, otherwise remove
import org.jacoco.report.IReportCoverageNodeVisitor;
import org.jacoco.report.IMultiReportOutput;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.JavaNames;
import org.jacoco.report.internal.ReportOutputFolder;
import org.jacoco.report.internal.html.HTMLElement;
import org.jacoco.report.internal.html.resources.Resources;
import org.jacoco.report.internal.html.table.BarColumn;
import org.jacoco.report.internal.html.table.CounterColumn;
import org.jacoco.report.internal.html.table.LabelColumn;
import org.jacoco.report.internal.html.table.PercentageColumn;
import org.jacoco.report.internal.html.table.Table;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * Formatter for HTML reports based on Freemarker templates.
 * Implements all visitor interfaces directly.
 */
public class FreemarkerFormatter implements IReportVisitor, IReportGroupVisitor, IReportCoverageNodeVisitor {

    private final Configuration cfg;
    private IMultiReportOutput output;
    private final JavaNames javaNames = new JavaNames();
    private Locale locale = Locale.getDefault();
    private String footerText = "";
    private String outputEncoding = "UTF-8";
    private Resources resources;
    private String currentPath = ""; // Stores the current relative path for links

    // To store items for the current group/package page
    private List<Map<String, Object>> currentGroupItems;
    private ICounter currentGroupCounter;


    /**
     * Creates a new formatter.
     */
    public FreemarkerFormatter() {
        cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setClassForTemplateLoading(getClass(), "freemarker_templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER); // Rethrow for better debugging
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setLocale(this.locale);
        cfg.setOutputEncoding(this.outputEncoding);
    }

    private void processTemplate(final String templateName, final Map<String, Object> dataModel, final String outputFileName) throws IOException {
        final Template template = cfg.getTemplate(templateName);
        try (final Writer writer = new OutputStreamWriter(output.createFile(outputFileName), outputEncoding)) {
            template.process(dataModel, writer);
        } catch (final TemplateException e) {
            throw new IOException("Error processing Freemarker template: " + templateName, e);
        }
    }

    private String getCoverageBar(ICounter counter) {
	// Basic HTML bar, can be improved with CSS in the template
	if (counter.getTotalCount() == 0) return "<div class='bar empty'></div>";
	double coveredRatio = counter.getCoveredRatio();
	int coveredPercent = (int) (coveredRatio * 100);
	return String.format(locale, "<div class='bar'><div class='covered' style='width: %d%%;'></div></div>", coveredPercent);
    }


    // === IReportVisitor Implementation ===

    public void init(final IMultiReportOutput output) throws IOException {
        this.output = output;
        this.resources = new Resources(new ReportOutputFolder(output)); // Assuming ReportOutputFolder can adapt IMultiReportOutput
        // If ReportOutputFolder strictly needs a File based root, this needs adjustment or a new adapter for IMultiReportOutput.
        // For now, let's assume it's adaptable or we handle resource copying manually.
    }

    public void visitInfo(final List<SessionInfo> sessionInfos, final Collection<Object> executionData) throws IOException { // Changed List<Object> to List<SessionInfo>
        final Map<String, Object> root = new HashMap<>();
        List<Map<String, String>> sessionData = new ArrayList<>();
        for (SessionInfo info : sessionInfos) {
            Map<String, String> session = new HashMap<>();
            session.put("id", info.getId());
            session.put("startTime", info.getStartTimeStamp().toString()); // Format as needed
            session.put("dumpTime", info.getDumpTimeStamp().toString());   // Format as needed
            sessionData.add(session);
        }
        root.put("sessions", sessionData);
        root.put("footerText", footerText);
        root.put("index_link", "index.html"); // Link back to main page
        processTemplate("sessions_page.ftl", root, "sessions.html");
    }

    public void visitBundle(final IBundleCoverage bundle, final ISourceFileLocator locator) throws IOException {
        this.currentPath = ""; // Root path
        this.currentGroupItems = new ArrayList<>();
        this.currentGroupCounter = bundle.getInstructionCounter(); // Overall bundle counter

        for (final IPackageCoverage pkg : bundle.getPackages()) {
            visitPackage(pkg, locator); // This will populate currentGroupItems
        }

        final Map<String, Object> root = new HashMap<>();
        root.put("bundleName", bundle.getName());
        root.put("overall_coverage", getCoveragePercentage(bundle.getInstructionCounter()));
        root.put("coverage_data", currentGroupItems);
        root.put("footerText", footerText);
        // Add breadcrumbs if necessary, for main page it's just the bundle name
        processTemplate("main_page.ftl", root, "index.html");

        // After processing bundle, reset items for next potential bundle (if any)
        this.currentGroupItems = null;
    }

    public IReportGroupVisitor visitGroup(final String name) throws IOException {
        // Groups are logical constructs, path is based on them.
        // This method might be called recursively for nested groups.
        // For simplicity, current JaCoCo structure is Bundle -> Package -> Class.
        // If groups are introduced above packages, this logic needs refinement.
        // Assuming for now that visitGroup is for top-level "group of packages" if ever used,
        // or could be identified with visitBundle's handling of packages.
        // The current IReportVisitor structure doesn't clearly distinguish visitGroup from visitBundle's iteration.
        // Let's assume this is for a named group within a bundle, if such a structure exists.
        // If not, this method might not be heavily used by the standard JaCoCo analysis structure.

        // For now, let's make it behave like a new sub-directory and page.
        String oldPath = this.currentPath;
        // TODO: Sanitize 'name' if it can contain complex characters unsafe for directory names.
        this.currentPath = oldPath + name + "/";

        this.currentGroupItems = new ArrayList<>();
        // currentGroupCounter would need to be passed or calculated for this specific group
        // This requires the IGroupCoverage node if groups are more than just paths.
        // For now, we don't have a specific IGroupCoverage object here.

        // This method should return an IReportGroupVisitor, which is `this`.
        return this;
    }

    public void visitEnd() throws IOException {
        copyAllResources();
        output.close();
    }

    private void copyAllResources() throws IOException {
        final String targetFolder = "jacoco-resources";
        output.ensureFolder(targetFolder + "/"); // Ensure trailing slash for folder

        // List of resources from org.jacoco.report.internal.html.resources.Resources.java
        final String[] resourceNames = {
            "report.css", "prettify.css", "prettify.js", "sort.js",
            "redbar.gif", "greenbar.gif", "report.gif", "group.gif",
            "bundle.gif", "package.gif", "source.gif", "class.gif",
            "method.gif", "session.gif", "sort.gif", "up.gif", "down.gif",
            "branchfc.gif", "branchnc.gif", "branchpc.gif"
        };

        for (final String name : resourceNames) {
            // Resources are in org.jacoco.report.internal.html.resources package
            final String resourcePath = "/org/jacoco/report/internal/html/resources/" + name;
            try (final InputStream in = FreemarkerFormatter.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // This would be an issue, means resource path is wrong or not in classpath
                    System.err.println("Warning: Resource not found: " + resourcePath);
                    continue;
                }
                try (final OutputStream out = output.createFile(targetFolder + "/" + name)) {
                    final byte[] buffer = new byte[1024]; // Increased buffer size
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }
    }


    // === IReportGroupVisitor Implementation ===
    // (visitBundle and visitGroup are from IReportVisitor, already implemented above)
    // visitEnd is also from IReportVisitor

    public IReportCoverageNodeVisitor visitPackage(final IPackageCoverage p, final ISourceFileLocator l) throws IOException {
        String originalPath = this.currentPath;
        String packagePath = javaNames.getFolderSimpleName(p.getName());
        this.currentPath = originalPath + packagePath + "/";
        this.output.ensureFolder(this.currentPath);

        List<Map<String, Object>> packagePageItems = new ArrayList<>();
        ICounter packageCounter = p.getInstructionCounter();

        for (final IClassCoverage c : p.getClasses()) {
            // visitClass will not generate a page, but will add its info to packagePageItems
            // For source_page.ftl, visitSourceFile handles page generation.
            // Here, we are collecting data for the group_page.ftl (representing the package listing classes)
            Map<String, Object> classItem = new HashMap<>();
            String sourceFileName = c.getSourceFileName();
            classItem.put("name", javaNames.getClassName(c.getName(), c.getPackageName(), c.getSignature(), c.getSuperName(), c.getInterfaceNames()));
            classItem.put("link", sourceFileName != null ? javaNames.getSourceSimpleName(sourceFileName) + ".html" : "#");
            classItem.put("elementType", "class");

            classItem.put("instructionCoverage", getCoveragePercentage(c.getInstructionCounter()));
            classItem.put("missedInstructions", c.getInstructionCounter().getMissedCount());
            classItem.put("totalInstructions", c.getInstructionCounter().getTotalCount());
            classItem.put("branchCoverage", getCoveragePercentage(c.getBranchCounter()));
            classItem.put("missedBranches", c.getBranchCounter().getMissedCount());
            classItem.put("totalBranches", c.getBranchCounter().getTotalCount());
            classItem.put("missedLines", c.getLineCounter().getMissedCount());
            classItem.put("missedComplexity", c.getComplexityCounter().getMissedCount());
            classItem.put("totalComplexity", c.getComplexityCounter().getTotalCount());
            classItem.put("missedMethods", c.getMethodCounter().getMissedCount());
            // For the "Classes" column on a package page, it refers to this specific class's status
            classItem.put("missedClasses", c.getClassCounter().getMissedCount()); // 1 if this class is missed, 0 otherwise

            packagePageItems.add(classItem);

            // Important: Call visitClass to potentially trigger source file processing for this class
            visitClass(c, l);
        }

        // Data for the current package's group_page.ftl
        final Map<String, Object> root = new HashMap<>();
        root.put("group_name", javaNames.getPackageName(p.getName()));
        root.put("overall_coverage", getCoveragePercentage(packageCounter));
        root.put("coverage_data", packagePageItems); // List of classes for this package page
        root.put("footerText", footerText);
        // Link back to the main index.html (bundle page)
        // currentPath is "bundleName/packageName/", so need to go up one level for jacoco-resources, two for main index.
        String relativePathToRoot = "../";
        // If currentPath is just "packageName/", then this is fine.
        // If currentPath can be "group/packageName/", then this needs to be more dynamic.
        // For now, assuming package is one level below bundle page.
        root.put("index_link", relativePathToRoot + "index.html");
        // TODO: Breadcrumbs for group page (Package > Class)
        // Example:
        // List<Map<String, String>> breadcrumbs = new ArrayList<>();
        // breadcrumbs.add(Map.of("name", "Overall", "link", relativePathToRoot + "index.html"));
        // breadcrumbs.add(Map.of("name", javaNames.getPackageName(p.getName()), "link", "#")); // Current page
        // root.put("breadcrumbs", breadcrumbs);


        processTemplate("group_page.ftl", root, this.currentPath + "index.html");

        // Add this package to the parent's list (e.g., bundle's list)
        if (this.currentGroupItems != null && !originalPath.equals(this.currentPath)) {
            Map<String, Object> packageSummaryItem = new HashMap<>();
            packageSummaryItem.put("name", javaNames.getPackageName(p.getName()));
            packageSummaryItem.put("link", packagePath + "/index.html"); // Relative to current base (bundle page)
            packageSummaryItem.put("elementType", "package");

            packageSummaryItem.put("instructionCoverage", getCoveragePercentage(p.getInstructionCounter()));
            packageSummaryItem.put("missedInstructions", p.getInstructionCounter().getMissedCount());
            packageSummaryItem.put("totalInstructions", p.getInstructionCounter().getTotalCount());
            packageSummaryItem.put("branchCoverage", getCoveragePercentage(p.getBranchCounter()));
            packageSummaryItem.put("missedBranches", p.getBranchCounter().getMissedCount());
            packageSummaryItem.put("totalBranches", p.getBranchCounter().getTotalCount());

            packageSummaryItem.put("missedLines", p.getLineCounter().getMissedCount());
            // Assuming complexity is method complexity. Summing it up for package.
            long missedComplexity = 0;
            long totalComplexity = 0;
            long missedMethods = 0;
            long totalMethods = 0; // Should be p.getMethodCounter().getTotalCount() but methods are within classes
            long missedClasses = 0;
            long totalClasses = p.getClassCounter().getTotalCount();


            for (IClassCoverage c : p.getClasses()) {
                if (c.getClassCounter().getMissedCount() > 0) {
                    missedClasses++;
                }
                missedComplexity += c.getComplexityCounter().getMissedCount();
                totalComplexity += c.getComplexityCounter().getTotalCount();
                missedMethods += c.getMethodCounter().getMissedCount();
                totalMethods +=c.getMethodCounter().getTotalCount();
            }
            packageSummaryItem.put("missedComplexity", missedComplexity);
            packageSummaryItem.put("totalComplexity", totalComplexity);
            packageSummaryItem.put("missedMethods", missedMethods);
            // packageSummaryItem.put("totalMethods", totalMethods); // Template uses only missed
            packageSummaryItem.put("missedClasses", missedClasses);
            // packageSummaryItem.put("totalClasses", totalClasses); // Template uses only missed

            // Coverage bar data (optional, if template uses a generic bar)
            // packageSummaryItem.put("coverage_bar_instruction", getCoverageBar(p.getInstructionCounter()));
            // packageSummaryItem.put("coverage_bar_branch", getCoverageBar(p.getBranchCounter()));

            this.currentGroupItems.add(packageSummaryItem);
        }

        this.currentPath = originalPath; // Restore path
        return this; // Returns IReportCoverageNodeVisitor
    }

    // === IReportCoverageNodeVisitor Implementation ===
    // visitPackage is above

    public void visitClass(final IClassCoverage c, final ISourceFileLocator l) throws IOException {
        // Generally, class details are part of the package page or source file page.
        // It will call visitSourceFile if a source file is linked.
        if (c.getSourceFileName() != null && l != null) {
            visitSourceFile(c.getSourceFileName(), l.getSourceFile(c.getPackageName(), c.getSourceFileName()), c);
        }
        // It could also iterate through methods if method-level pages were desired (not in current templates)
        // for (final IMethodCoverage m : c.getMethods()) {
        // visitMethod(m, l);
        // }
    }

    public void visitSourceFile(final ISourceFileCoverage s, final ISourceFileLocator l) throws IOException {
         // This version of visitSourceFile is if we are iterating source files directly (e.g. from a package)
         // We need a version that takes IClassCoverage to link styling.
         // The template source_page.ftl is more geared towards showing a class's source.
         // For now, this might not be directly called by current flow which goes Class -> SourceFile.
         // If it is, it needs a class context or a different template.
    }

    // Custom method to handle source file rendering with class context
    private void visitSourceFile(final String fileName, final Reader reader, final IClassCoverage c) throws IOException {
        if (reader == null) return; // Source not found

        // Path for source file HTML page is within its package directory
        final String sourcePagePath = this.currentPath + javaNames.getSourceSimpleName(fileName) + ".html";

        final Map<String, Object> root = new HashMap<>();
        root.put("file_name", fileName);
        // Assuming group_name for source page refers to its package
        root.put("group_name", javaNames.getPackageName(c.getPackageName()));
        root.put("group_link", "index.html"); // Link to the package index page (currently in 'this.currentPath')
        root.put("footerText", footerText);
        // TODO: Breadcrumbs: e.g., Bundle > Package > SourceFile
        // List<Map<String, String>> breadcrumbs = new ArrayList<>();
        // breadcrumbs.add(Map.of("name", "Overall", "link", "../../index.html")); // Adjust link based on depth
        // breadcrumbs.add(Map.of("name", javaNames.getPackageName(c.getPackageName()), "link", "index.html"));
        // breadcrumbs.add(Map.of("name", fileName, "link", "#")); // Current page
        // root.put("breadcrumbs", breadcrumbs);


        List<Map<String,Object>> sourceLinesData = new ArrayList<>();
        try (java.io.BufferedReader bufferedReader = new java.io.BufferedReader(reader)) {
            String lineText;
            for (int lineNr = 1; (lineText = bufferedReader.readLine()) != null; lineNr++) {
                Map<String, Object> lineData = new HashMap<>();
                // HTML escape lineText? Freemarker usually does by default.
                // JaCoCo's existing HTML report does tab expansion.
                lineData.put("text", lineText.replace("\t", "    "));

                ILine lineCoverage = c.getLine(lineNr);
                lineData.put("css_class", getCssClassForLine(lineCoverage));
                // Optionally, pass instruction and branch details if template needs them
                // lineData.put("instruction_status", lineCoverage.getInstructionCounter().getStatus());
                // lineData.put("branch_status", lineCoverage.getBranchCounter().getStatus());
                sourceLinesData.add(lineData);
            }
        }
        root.put("source_lines", sourceLinesData);

        processTemplate("source_page.ftl", root, sourcePagePath);
    }

    private String getCssClassForLine(final ILine line) {
        if (line == null) return "pln"; // Should not happen if line numbers are correct

        final int status = line.getStatus();
        String cssClass;
        switch (status) {
            case ICounter.NOT_COVERED:
                cssClass = "nc"; // Not Covered
                break;
            case ICounter.PARTLY_COVERED:
                cssClass = "pc"; // Partly Covered (for branches)
                break;
            case ICounter.FULLY_COVERED:
                cssClass = "fc"; // Fully Covered
                break;
            case ICounter.EMPTY:
            default:
                cssClass = "pln"; // Plain, or for empty lines
                break;
        }
        // JaCoCo also has specific styles for lines with missed branches (e.g. "fc bnc" or "pc bpc")
        // For simplicity, direct status mapping first. Refine if branch indicators are separate in template.
        if (line.getBranchCounter().getTotalCount() > 0) {
            if (line.getBranchCounter().getMissedCount() > 0) {
                cssClass += " bnc"; // Branches Not Covered (or partly)
            } else {
                cssClass += " bfc"; // Branches Fully Covered
            }
        }
        return cssClass;
    }


    public void visitMethod(final IMethodCoverage m, final ISourceFileLocator l) throws IOException {
        // Method details are usually part of the source file view or class view.
        // Not generating a separate page for each method with current templates.
        // This would be called from visitClass if iterating methods.
    }


    // === Helper methods ===

    private String getCoveragePercentage(final ICounter counter) {
        if (counter.getTotalCount() == 0) {
            return "0.00"; // Consistent formatting
        }
        final double ratio = counter.getCoveredRatio();
        return String.format(locale, "%.2f", ratio * 100);
    }


    // === Setters from original class ===

    public void setLocale(final Locale locale) {
        this.locale = locale;
        cfg.setLocale(locale);
    }

    public void setFooterText(final String footerText) {
        this.footerText = footerText;
    }

    public void setOutputEncoding(final String outputEncoding) {
        this.outputEncoding = outputEncoding;
        cfg.setOutputEncoding(outputEncoding);
    }

    // Removed createVisitor method as FreemarkerFormatter now implements all visitor interfaces
}
