package org.jacoco.report;

import java.io.IOException;

import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

/**
 * A visitor for coverage data of different structural elements. This interface
 * is typically implemented by formatters that generate reports.
 *
 * @see IReportVisitor
 * @see IReportGroupVisitor
 */
public interface IReportCoverageNodeVisitor {

	/**
	 * Called to visit a package coverage node.
	 *
	 * @param packageCoverage
	 *            coverage data for the package
	 * @param locator
	 *            source file locator
	 * @return a visitor for the elements within this package, or
	 *         <code>null</code> if the contents of this package should not be
	 *         visited
	 * @throws IOException
	 *             if an error occurs while writing the report
	 */
	IReportCoverageNodeVisitor visitPackage(
			IPackageCoverage packageCoverage, ISourceFileLocator locator)
			throws IOException;

	/**
	 * Called to visit a class coverage node.
	 *
	 * @param classCoverage
	 *            coverage data for the class
	 * @param locator
	 *            source file locator
	 * @throws IOException
	 *             if an error occurs while writing the report
	 */
	void visitClass(IClassCoverage classCoverage, ISourceFileLocator locator)
			throws IOException;

	/**
	 * Called to visit a source file coverage node.
	 *
	 * @param sourceFileCoverage
	 *            coverage data for the source file
	 * @param locator
	 *            source file locator
	 * @throws IOException
	 *             if an error occurs while writing the report
	 */
	void visitSourceFile(ISourceFileCoverage sourceFileCoverage,
			ISourceFileLocator locator) throws IOException;

	/**
	 * Called to visit a method coverage node.
	 *
	 * @param methodCoverage
	 *            coverage data for the method
	 * @param locator
	 *            source file locator
	 * @throws IOException
	 *             if an error occurs while writing the report
	 */
	void visitMethod(IMethodCoverage methodCoverage,
			ISourceFileLocator locator) throws IOException;

}
