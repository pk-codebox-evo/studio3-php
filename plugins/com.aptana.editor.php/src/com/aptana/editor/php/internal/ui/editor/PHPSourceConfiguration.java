package com.aptana.editor.php.internal.ui.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.IPartitioningConfiguration;
import com.aptana.editor.common.ISourceViewerConfiguration;
import com.aptana.editor.common.scripting.IContentTypeTranslator;
import com.aptana.editor.common.scripting.QualifiedContentType;
import com.aptana.editor.common.text.rules.CommentScanner;
import com.aptana.editor.common.text.rules.CompositePartitionScanner;
import com.aptana.editor.common.text.rules.ISubPartitionScanner;
import com.aptana.editor.common.text.rules.PartitionerSwitchingIgnoreRule;
import com.aptana.editor.common.text.rules.SubPartitionScanner;
import com.aptana.editor.common.text.rules.ThemeingDamagerRepairer;
import com.aptana.editor.html.HTMLSourceConfiguration;
import com.aptana.editor.html.IHTMLConstants;
import com.aptana.editor.php.internal.contentAssist.PHPContentAssistProcessor;
import com.aptana.editor.php.internal.core.IPHPConstants;
import com.aptana.editor.php.internal.parser.HeredocRule;
import com.aptana.editor.php.internal.ui.editor.scanner.PHPCodeScanner;

public class PHPSourceConfiguration implements IPartitioningConfiguration, ISourceViewerConfiguration, IPHPConstants
{
	public static final String[] CONTENT_TYPES = new String[] { DEFAULT, PHP_SINGLE_LINE_COMMENT,
		PHP_DOC_COMMENT, PHP_MULTI_LINE_COMMENT, COMMAND, PHP_STRING_SINGLE, PHP_STRING_DOUBLE, PHP_HEREDOC,
		PHP_NOWDOC };/* REGULAR_EXPRESSION */

	private static final String[][] TOP_CONTENT_TYPES = new String[][] { { CONTENT_TYPE_PHP } };

	private IPredicateRule[] partitioningRules = new IPredicateRule[] {
			new EndOfLineRule("//", new Token(PHP_SINGLE_LINE_COMMENT)), //$NON-NLS-1$
			new EndOfLineRule("#", new Token(PHP_SINGLE_LINE_COMMENT)), //$NON-NLS-1$
			new PartitionerSwitchingIgnoreRule(new MultiLineRule("/*", "*/", new Token(PHP_MULTI_LINE_COMMENT), (char) 0, true)), //$NON-NLS-1$ //$NON-NLS-2$
			new PartitionerSwitchingIgnoreRule(new MultiLineRule("/**", "*/", new Token(PHP_DOC_COMMENT), (char) 0, true)), //$NON-NLS-1$ //$NON-NLS-2$
			new PartitionerSwitchingIgnoreRule(new MultiLineRule("\'", "\'", new Token(PHP_STRING_SINGLE), '\\', true)), //$NON-NLS-1$ //$NON-NLS-2$
			new PartitionerSwitchingIgnoreRule(new MultiLineRule("\"", "\"", new Token(PHP_STRING_DOUBLE), '\\', true)), //$NON-NLS-1$ //$NON-NLS-2$
			new PartitionerSwitchingIgnoreRule(new HeredocRule(new Token(PHP_HEREDOC), false)),
			new PartitionerSwitchingIgnoreRule(new HeredocRule(new Token(PHP_NOWDOC), true)),
	};

	private PHPCodeScanner codeScanner;
	private RuleBasedScanner singleLineCommentScanner;
	private RuleBasedScanner multiLineCommentScanner;
	// private RuleBasedScanner commandScanner;
	private RuleBasedScanner singleQuotedStringScanner;
	private RuleBasedScanner doubleQuotedStringScanner;
	private RuleBasedScanner heredocScanner;
	private RuleBasedScanner nowdocScanner;

	private RuleBasedScanner phpDocCommentScanner;

	private static PHPSourceConfiguration instance;

	static
	{
		IContentTypeTranslator c = CommonEditorPlugin.getDefault().getContentTypeTranslator();
		c.addTranslation(new QualifiedContentType(CONTENT_TYPE_HTML_PHP, IDocument.DEFAULT_CONTENT_TYPE),
				new QualifiedContentType("text.html.basic")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(CONTENT_TYPE_HTML_PHP, CONTENT_TYPE_PHP), new QualifiedContentType(
				"source.php", "source.php.embedded.block.html")); //$NON-NLS-1$ //$NON-NLS-2$
		c.addTranslation(new QualifiedContentType(CONTENT_TYPE_HTML_PHP, IHTMLConstants.CONTENT_TYPE_HTML),
				new QualifiedContentType("text.html.basic")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(PHP_STRING_SINGLE), new QualifiedContentType(
				"string.quoted.single.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(PHP_STRING_DOUBLE), new QualifiedContentType(
				"string.quoted.double.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(PHP_HEREDOC), new QualifiedContentType(
				"string.unquoted.heredoc.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(PHP_SINGLE_LINE_COMMENT), new QualifiedContentType(
				"comment.line.double-slash.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(PHP_DOC_COMMENT), new QualifiedContentType(
				"comment.block.documentation.phpdoc.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(PHP_MULTI_LINE_COMMENT), new QualifiedContentType("comment.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(CONTENT_TYPE_HTML_PHP, CompositePartitionScanner.START_SWITCH_TAG),
				new QualifiedContentType("punctuation.section.embedded.begin.php")); //$NON-NLS-1$
		c.addTranslation(new QualifiedContentType(CONTENT_TYPE_HTML_PHP, CompositePartitionScanner.END_SWITCH_TAG), new QualifiedContentType(
				"punctuation.section.embedded.end.php")); //$NON-NLS-1$
	}
	
	private PHPSourceConfiguration() {
	}

	public static PHPSourceConfiguration getDefault()
	{
		if (instance == null)
		{
			instance = new PHPSourceConfiguration();
		}
		return instance;
	}

	/**
	 * @see com.aptana.editor.common.IPartitioningConfiguration#getContentTypes()
	 */
	public String[] getContentTypes()
	{
		return CONTENT_TYPES;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.ITopContentTypesProvider#getTopContentTypes()
	 */
	public String[][] getTopContentTypes()
	{
		return TOP_CONTENT_TYPES;
	}

	/**
	 * @see com.aptana.editor.common.IPartitioningConfiguration#getPartitioningRules()
	 */
	public IPredicateRule[] getPartitioningRules()
	{
		return partitioningRules;
	}

	/**
	 * @see com.aptana.editor.common.IPartitioningConfiguration#createSubPartitionScanner()
	 */
	public ISubPartitionScanner createSubPartitionScanner()
	{
		return new SubPartitionScanner(partitioningRules, CONTENT_TYPES, new Token(DEFAULT));
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.IPartitioningConfiguration#getDocumentDefaultContentType()
	 */
	public String getDocumentContentType(String contentType)
	{
		if (contentType.startsWith(PREFIX))
		{
			return CONTENT_TYPE_PHP;
		}
		return null;
	}

	/**
	 * @see com.aptana.editor.common.ISourceViewerConfiguration#setupPresentationReconciler(org.eclipse.jface.text.presentation.PresentationReconciler,
	 *      org.eclipse.jface.text.source.ISourceViewer)
	 */
	public void setupPresentationReconciler(PresentationReconciler reconciler, ISourceViewer sourceViewer)
	{
		DefaultDamagerRepairer dr = new ThemeingDamagerRepairer(getCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		reconciler.setDamager(dr, DEFAULT);
		reconciler.setRepairer(dr, DEFAULT);

		dr = new ThemeingDamagerRepairer(getSingleLineCommentScanner());
		reconciler.setDamager(dr, PHP_SINGLE_LINE_COMMENT);
		reconciler.setRepairer(dr, PHP_SINGLE_LINE_COMMENT);

		dr = new ThemeingDamagerRepairer(getPhpDocCommentScanner());
		reconciler.setDamager(dr, PHP_DOC_COMMENT);
		reconciler.setRepairer(dr, PHP_DOC_COMMENT);
		
		dr = new ThemeingDamagerRepairer(getMultiLineCommentScanner());
		reconciler.setDamager(dr, PHP_MULTI_LINE_COMMENT);
		reconciler.setRepairer(dr, PHP_MULTI_LINE_COMMENT);

		// dr = new ThemeingDamagerRepairer(getCommandScanner());
		// reconciler.setDamager(dr, PHPSourceConfiguration.COMMAND);
		// reconciler.setRepairer(dr, PHPSourceConfiguration.COMMAND);

		dr = new ThemeingDamagerRepairer(getSingleQuotedStringScanner());
		reconciler.setDamager(dr, PHP_STRING_SINGLE);
		reconciler.setRepairer(dr, PHP_STRING_SINGLE);

		dr = new ThemeingDamagerRepairer(getDoubleQuotedStringScanner());
		reconciler.setDamager(dr, PHP_STRING_DOUBLE);
		reconciler.setRepairer(dr, PHP_STRING_DOUBLE);

		dr = new ThemeingDamagerRepairer(getHeredocScanner());
		reconciler.setDamager(dr, PHP_HEREDOC);
		reconciler.setRepairer(dr, PHP_HEREDOC);
		
		dr = new ThemeingDamagerRepairer(getNowdocScanner());
		reconciler.setDamager(dr, PHP_NOWDOC);
		reconciler.setRepairer(dr, PHP_NOWDOC);

	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.common.ISourceViewerConfiguration#getContentAssistProcessor(com.aptana.editor.common.AbstractThemeableEditor, java.lang.String)
	 */
	public IContentAssistProcessor getContentAssistProcessor(AbstractThemeableEditor editor, String contentType)
	{
		if (contentType.startsWith(IPHPConstants.PREFIX))
		{
			return new PHPContentAssistProcessor(editor);
		}
		// In any other case, call the HTMLSourceViewerConfiguration to compute the assist processor.
		return HTMLSourceConfiguration.getDefault().getContentAssistProcessor(editor, contentType);
	}

	private ITokenScanner getCodeScanner()
	{
		if (codeScanner == null)
		{
			codeScanner = new PHPCodeScanner();
		}
		return codeScanner;
	}

	private ITokenScanner getPhpDocCommentScanner()
	{
		if (phpDocCommentScanner == null)
		{
			phpDocCommentScanner = new CommentScanner(getToken("comment.block.documentation.phpdoc.php")); //$NON-NLS-1$
		}
		return phpDocCommentScanner;
	}
	
	private ITokenScanner getMultiLineCommentScanner()
	{
		if (multiLineCommentScanner == null)
		{
			multiLineCommentScanner = new CommentScanner(getToken("comment.php")); //$NON-NLS-1$
		}
		return multiLineCommentScanner;
	}

	private ITokenScanner getSingleLineCommentScanner()
	{
		if (singleLineCommentScanner == null)
		{
			singleLineCommentScanner = new CommentScanner(getToken("comment.line.number-sign.php")); //$NON-NLS-1$
		}
		return singleLineCommentScanner;
	}

	private ITokenScanner getSingleQuotedStringScanner()
	{
		if (singleQuotedStringScanner == null)
		{
			singleQuotedStringScanner = new RuleBasedScanner();
			singleQuotedStringScanner.setDefaultReturnToken(getToken("string.quoted.single.php")); //$NON-NLS-1$
		}
		return singleQuotedStringScanner;
	}

	private ITokenScanner getDoubleQuotedStringScanner()
	{
		if (doubleQuotedStringScanner == null)
		{
			doubleQuotedStringScanner = new RuleBasedScanner();
			doubleQuotedStringScanner.setDefaultReturnToken(getToken("string.quoted.double.php")); //$NON-NLS-1$
		}
		return doubleQuotedStringScanner;
	}

	private ITokenScanner getHeredocScanner()
	{
		if (heredocScanner == null)
		{
			heredocScanner = new RuleBasedScanner();
			heredocScanner.setDefaultReturnToken(getToken("string.unquoted.heredoc.php")); //$NON-NLS-1$
		}
		return heredocScanner;
	}

	private ITokenScanner getNowdocScanner()
	{
		if (nowdocScanner == null)
		{
			nowdocScanner = new RuleBasedScanner();
			nowdocScanner.setDefaultReturnToken(getToken("string.unquoted.nowdoc.php")); //$NON-NLS-1$
		}
		return nowdocScanner;
	}

	private IToken getToken(String tokenName)
	{
		return new Token(tokenName);
	}
}
