package org.l2x6.editorconfig.validator.xml;

import java.util.Deque;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.l2x6.editorconfig.core.EditorConfigService.IndentStyle;
import org.l2x6.editorconfig.core.EditorConfigService.OptionMap;
import org.l2x6.editorconfig.core.EditorConfigService.WellKnownKey;
import org.l2x6.editorconfig.core.Location;
import org.l2x6.editorconfig.core.Resource;
import org.l2x6.editorconfig.core.Violation;
import org.l2x6.editorconfig.core.ViolationHandler;
import org.l2x6.editorconfig.format.Delete;
import org.l2x6.editorconfig.format.Edit;
import org.l2x6.editorconfig.format.Insert;
import org.l2x6.editorconfig.parser.xml.XmlParser.AttributeContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.ChardataContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.CommentContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.ContentContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.DocumentContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.ElementContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.EndNameContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.MiscContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.ProcessingInstructionContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.PrologContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.ReferenceContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.SeaWsContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.StartEndNameContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.StartNameContext;
import org.l2x6.editorconfig.parser.xml.XmlParser.TextContext;
import org.l2x6.editorconfig.parser.xml.XmlParserListener;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A {@link DefaultHandler} implementation that detects formatting violations and reports them to the supplied
 * {@link #violationHandler}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class FormatParserListener
    implements XmlParserListener
{

    /**
     * An entry that can be stored on a stack
     */
    private static class ElementEntry
    {
        private final String elementName;

        private final FormatParserListener.Indent expectedIndent;

        private final FormatParserListener.Indent foundIndent;

        public ElementEntry( String elementName, FormatParserListener.Indent foundIndent )
        {
            super();
            this.elementName = elementName;
            this.foundIndent = foundIndent;
            this.expectedIndent = foundIndent;
        }

        public ElementEntry( String elementName, FormatParserListener.Indent foundIndent,
                             FormatParserListener.Indent expectedIndent )
        {
            super();
            this.elementName = elementName;
            this.foundIndent = foundIndent;
            this.expectedIndent = expectedIndent;
        }

        @Override
        public String toString()
        {
            return "<" + elementName + "> " + foundIndent;
        }
    }

    /**
     * An indent occurrence within a file characterized by {@link #lineNumber} and {@link #size}.
     */
    private static class Indent
    {

        /**
         * An {@link Indent} usable at the beginning of a typical XML file.
         */
        public static final FormatParserListener.Indent START = new Indent( 1, 0 );

        /**
         * The line number where this {@link Indent} occurs. The first line number in a file is {@code 1}.
         */
        private final int lineNumber;

        /** The number of spaces in this {@link Indent}. */
        private final int size;

        public Indent( int lineNumber, int size )
        {
            super();
            this.lineNumber = lineNumber;
            this.size = size;
        }

        @Override
        public String toString()
        {
            return "Indent [size=" + size + ", lineNumber=" + lineNumber + "]";
        }
    }

    static class LastTerminalFinder
        extends AbstractParseTreeVisitor<Object>
    {

        private TerminalNode lastTerminal;

        public TerminalNode getLastTerminal()
        {
            return lastTerminal;
        }

        @Override
        public Object visitTerminal( TerminalNode node )
        {

            lastTerminal = node;
            return null;
        }

    }

    private final StringBuilder charBuffer = new StringBuilder();

    private int charLineNumber;

    /** The file being checked */
    private final Resource file;

    private FormatParserListener.Indent lastIndent = Indent.START;

    /** The element stack */
    private Deque<FormatParserListener.ElementEntry> stack =
        new java.util.ArrayDeque<FormatParserListener.ElementEntry>();

    /** The {@link ViolationHandler} for reporting found violations */
    private final ViolationHandler violationHandler;

    private final char indentChar;

    private final int indentSize;

    public FormatParserListener( Resource file, OptionMap options,
                                 ViolationHandler violationHandler)
    {
        super();
        this.file = file;
        this.indentChar = options.get(WellKnownKey.indent_style, IndentStyle.getDefault()).getindentChar();
        this.indentSize = options.get(WellKnownKey.indent_size, Integer.valueOf(2)).intValue();
        this.violationHandler = violationHandler;
    }

    @Override
    public void visitTerminal( TerminalNode node )
    {
    }

    @Override
    public void visitErrorNode( ErrorNode node )
    {
    }

    @Override
    public void enterEveryRule( ParserRuleContext ctx )
    {
    }

    @Override
    public void exitEveryRule( ParserRuleContext ctx )
    {
    }

    @Override
    public void enterDocument( DocumentContext ctx )
    {

    }

    @Override
    public void exitDocument( DocumentContext ctx )
    {
    }

    @Override
    public void enterProlog( PrologContext ctx )
    {
        flushWs();
    }

    @Override
    public void exitProlog( PrologContext ctx )
    {
        flushWs();
    }

    @Override
    public void enterContent( ContentContext ctx )
    {
    }

    @Override
    public void exitContent( ContentContext ctx )
    {
    }

    @Override
    public void enterElement( ElementContext ctx )
    {
    }

    @Override
    public void exitElement( ElementContext ctx )
    {
    }

    @Override
    public void enterReference( ReferenceContext ctx )
    {
    }

    @Override
    public void exitReference( ReferenceContext ctx )
    {
    }

    @Override
    public void enterAttribute( AttributeContext ctx )
    {
    }

    @Override
    public void exitAttribute( AttributeContext ctx )
    {
    }

    @Override
    public void enterChardata( ChardataContext ctx )
    {
    }

    @Override
    public void exitChardata( ChardataContext ctx )
    {
    }

    @Override
    public void enterMisc( MiscContext ctx )
    {
    }

    @Override
    public void exitMisc( MiscContext ctx )
    {
    }

    @Override
    public void enterComment( CommentContext ctx )
    {
    }

    @Override
    public void exitComment( CommentContext ctx )
    {
        flushWs();
    }

    @Override
    public void enterProcessingInstruction( ProcessingInstructionContext ctx )
    {
    }

    @Override
    public void exitProcessingInstruction( ProcessingInstructionContext ctx )
    {
        flushWs();
    }

    @Override
    public void enterSeaWs( SeaWsContext ctx )
    {
    }

    @Override
    public void exitSeaWs( SeaWsContext ctx )
    {
        consumeText( ctx );
    }

    /**
     * Sets {@link lastIndent} based on {@link #charBuffer} and resets {@link #charBuffer}.
     */
    private void flushWs()
    {
        int indentLength = 0;
        int len = charBuffer.length();
        /*
         * Count characters from end of ignorable whitespace to first end of line we hit
         */
        for ( int i = len - 1; i >= 0; i-- )
        {
            char ch = charBuffer.charAt( i );
            switch ( ch )
            {
                case '\n':
                case '\r':
                    lastIndent = new Indent( charLineNumber, indentLength );
                    charBuffer.setLength( 0 );
                    return;
                case ' ':
                case '\t':
                    indentLength++;
                    break;
                default:
                    /*
                     * No end of line foundIndent in the trailing whitespace. Leave the foundIndent from previous
                     * ignorable whitespace unchanged
                     */
                    charBuffer.setLength( 0 );
                    return;
            }
        }
    }

    @Override
    public void enterStartName( StartNameContext ctx )
    {
        flushWs();
        final String qName = ctx.getText();
        ElementEntry currentEntry = new ElementEntry( qName, lastIndent );
        if ( !stack.isEmpty() )
        {
            ElementEntry parentEntry = stack.peek();
            /*
             * note that we use parentEntry.expectedIndent rather than parentEntry.foundIndent this is to make the
             * messages more useful
             */
            int indentDiff = currentEntry.foundIndent.size - parentEntry.expectedIndent.size;
            int expectedIndent = parentEntry.expectedIndent.size + indentSize;
            if ( indentDiff == 0 && currentEntry.foundIndent.lineNumber == parentEntry.foundIndent.lineNumber )
            {
                /*
                 * Zero foundIndent acceptable only if current is on the same line as parent This is OK, therefore do
                 * nothing
                 */
            }
            else if ( indentDiff != indentSize )
            {
                /* generally unexpected foundIndent */
                int opValue = expectedIndent - currentEntry.foundIndent.size;

                final Edit fix;
                final int len = Math.abs( opValue );
                final Token start = ctx.getStart();
                int col = start.getCharPositionInLine() //
                                + 1 // because getCharPositionInLine() is zero based
                                - 1 // because we want the column of '<' while we are on the first char of the name
                                ;
                if ( opValue > 0 )
                {
                    fix = Insert.repeat( indentChar, len );
                }
                else
                {
                    fix = new Delete( len );
                    col -= len;
                }
                final Location loc = new Location( start.getLine(), col );

                Violation violation = new Violation( file, loc, fix );
                violationHandler.handle( violation );

                /* reset the expected indent in the entry we'll push */
                currentEntry =
                    new ElementEntry( qName, lastIndent, new Indent( lastIndent.lineNumber, expectedIndent ) );
            }
        }
        stack.push( currentEntry );
    }

    @Override
    public void exitStartName( StartNameContext ctx )
    {
    }

    @Override
    public void enterEndName( EndNameContext ctx )
    {
        flushWs();
        final String qName = ctx.getText();
        if ( stack.isEmpty() )
        {
            final Token start = ctx.getStart();
            throw new IllegalStateException( "Stack must not be empty when closing the element " + qName
                + " around line " + start.getLine() + " and column " + ( start.getCharPositionInLine() + 1 ) );
        }
        ElementEntry startEntry = stack.pop();
        int indentDiff = lastIndent.size - startEntry.expectedIndent.size;
        int expectedIndent = startEntry.expectedIndent.size;
        if ( lastIndent.lineNumber != startEntry.foundIndent.lineNumber && indentDiff != 0 )
        {
            /*
             * diff should be zero unless we are on the same line as start element
             */
            int opValue = expectedIndent - lastIndent.size;
            final Edit fix;
            final int len = Math.abs( opValue );
            final Token start = ctx.getStart();
            int col = start.getCharPositionInLine() //
                            + 1 // because getCharPositionInLine() is zero based
                            - 2 // because we want the column of '<' while we are on the first char of the name
                            ;
            if ( opValue > 0 )
            {
                fix = Insert.repeat( indentChar, len );
            }
            else
            {
                fix = new Delete( len );
                col -= len ;
            }
            final Location loc = new Location( start.getLine(), col );

            Violation violation = new Violation( file, loc, fix );
            violationHandler.handle( violation );
        }
    }

    @Override
    public void exitEndName( EndNameContext ctx )
    {
    }

    @Override
    public void enterStartEndName( StartEndNameContext ctx )
    {
        //System.out.println( "<" + ctx.getText() + "/>" );
    }

    @Override
    public void exitStartEndName( StartEndNameContext ctx )
    {
    }

    @Override
    public void enterText( TextContext ctx )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void exitText( TextContext ctx )
    {
        consumeText( ctx );
    }

    private void consumeText( ParserRuleContext ctx )
    {
        charBuffer.append( ctx.getText() );
        charLineNumber = ctx.getStop().getLine();
    }

}