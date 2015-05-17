/* The following code was generated by JFlex 1.4.3 on 03.05.15 13:19 */

package com.perl5.lang.perl.lexer;


import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.perl5.lang.perl.util.PerlPackageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerlLexer extends PerlLexerGenerated{

	protected IElementType lastSignificantTokenType;
	protected String lastSignificantToken;
	protected IElementType lastTokenType;

	public PerlLexer(java.io.Reader in) {
		super(in);
	}

	public IElementType advance() throws IOException{
//		System.out.printf("Advances from %d %d\n", getTokenStart(), yystate());
		IElementType tokenType = super.advance();

		lastTokenType = tokenType;
		if( tokenType != TokenType.NEW_LINE_INDENT
			&& tokenType != TokenType.WHITE_SPACE
			&& tokenType != PERL_COMMENT
			&& tokenType != PERL_COMMENT_BLOCK
			&& tokenType != PERL_POD
		)
		{
			lastSignificantTokenType = tokenType;
			lastSignificantToken = yytext().toString();

			if( yystate() == 0 && tokenType != PERL_SEMI) // to enshure proper highlighting reparsing
				yybegin(LEX_CODE);
		}

		return tokenType;
	}

	public void reset(CharSequence buf, int start, int end, int initialState)
	{
//		if(end > 0)
//			System.out.printf("Reset to %d %d %d `%c`\n", start, end, initialState, buf.charAt(start));
		super.reset(buf,start,end,initialState);
		lastTokenType = null;
		lastSignificantTokenType = null;
	}

	/**
	 * Forces push back and reparsing
	 * @param newState exclusive state for re-parsing specific constructions
	 */
	public void startCustomBlock(int newState)
	{
		yypushback(yylength());
		pushState();
		yybegin(newState);
	}

	/**
	 * Ends custom block parsing
	 */
	public void endCustomBlock()
	{
		popState();
	}

	/**
	 *  States stack
	 **/
	private final Stack<Integer> stateStack = new Stack<Integer>();

	public void pushState()
	{
		stateStack.push(yystate());
	}

	public void popState()
	{
		setState(stateStack.pop());
	}

	/**
	 *  Quote-like, transliteration and regexps common part
	 */
	public boolean allowSharp = true;
	public char charOpener;
	public char charCloser;
	public int stringContentStart;
	public boolean isEscaped = false;

	public int sectionsNumber = 0; 	// number of sections one or two
	public int currentSectionNumber = 0; // current section

	public final LinkedList<CustomToken> tokensList = new LinkedList<CustomToken>();

	private IElementType restoreToken( CustomToken token)
	{
		setTokenStart(token.getTokenStart());
		setTokenEnd(token.getTokenEnd());
		return token.getTokenType();
	}

	/**
	 * Disallows sharp delimiter on space occurance for quote-like operations
	 * @return whitespace token type
	 */
	public IElementType processOpenerWhiteSpace()
	{
		allowSharp = false;
		return TokenType.WHITE_SPACE;
	}

	/**
	 *  Reading tokens from parsed queue, setting start and end and returns them one by one
	 * @return token type or null if queue is empty
	 */
	public IElementType getParsedToken()
	{
		if(tokensList.size() == 0 )
		{
			popState();
			yypushback(1); // no tokens in this lex state, push back
			return null;
		}
		else
		{
			return restoreToken(tokensList.removeFirst());
		}
	}

	/**
	 *	Regex processor qr{} m{} s{}{}
	 **/
	String regexCommand = null;

	// guess if this is a division or started regex
	public IElementType processDiv()
	{
		if(	// seems regex, @todo map types and words
			lastSignificantTokenType == PERL_OPERATOR
			|| lastSignificantTokenType == PERL_LPAREN
			|| lastSignificantTokenType == PERL_LBRACE
			|| lastSignificantTokenType == PERL_LBRACK
			|| lastSignificantTokenType == PERL_SEMI
			|| lastSignificantToken.equals("return")
			|| lastSignificantToken.equals("split")
			|| lastSignificantToken.equals("if")
			|| lastSignificantToken.equals("unless")
			|| lastSignificantToken.equals("grep")
			|| lastSignificantToken.equals("map")
		)
		{
			allowSharp = true;
			isEscaped = false;
			regexCommand = "m";
			sectionsNumber = 1;

			pushState();
			yypushback(1);
			yybegin(LEX_REGEX_OPENER);

			return null;
		}
		else
		{
			if( !isLastToken() && getBuffer().charAt(getNextTokenStart()) == '/')
			{
				setTokenEnd(getNextTokenStart()+1);
				return PERL_OPERATOR;
			}
			else
			{
				return PERL_OPERATOR;
			}
		}
	}


	/**
	 * Sets up regex parser
	 * @return command keyword
	 */
	public IElementType processRegexOpener()
	{
		allowSharp = true;
		isEscaped = false;
		regexCommand = yytext().toString();

		if( "s".equals(regexCommand) )	// two sections s
			sectionsNumber = 2;
		else						// one section qr m
			sectionsNumber = 1;

		pushState();
		yybegin(LEX_REGEX_OPENER);
		return PERL_KEYWORD;
	}

	/**
	 *  Parses regexp from the current position (opening delimiter) and preserves tokens in tokensList
	 *  REGEX_MODIFIERS = [msixpodualgcer]
	 *  @return opening delimiter type
	 */
	public IElementType parseRegex()
	{
		tokensList.clear();

		CharSequence buffer = getBuffer();
		int bufferEnd = getBufferEnd();

		// find block 1
		RegexBlock firstBlock = RegexBlock.parseBlock(buffer, getTokenStart() + 1, bufferEnd, yytext().charAt(0));

		if( firstBlock == null )
		{
//			System.err.println("Stop after first block");
			yybegin(YYINITIAL);
			return PERL_REGEX_QUOTE;
		}
		int currentOffset = firstBlock.getEndOffset();

		// find block 2
		ArrayList<CustomToken> betweenBlocks = new ArrayList<CustomToken>();
		RegexBlock secondBLock = null;
		CustomToken secondBlockOpener = null;

		if( sectionsNumber == 2 && currentOffset < bufferEnd )
		{
			if(firstBlock.hasSameQuotes())
			{
				secondBLock = RegexBlock.parseBlock(buffer, currentOffset, bufferEnd, firstBlock.getOpeningQuote());
			}
			else
			{
				// spaces and comments between if {}, fill betweenBlock
				while( true )
				{
					char currentChar = buffer.charAt(currentOffset);
					if( RegexBlock.isWhiteSpace(currentChar) )	// white spaces
					{
						int whiteSpaceStart = currentOffset;
						while( RegexBlock.isWhiteSpace(buffer.charAt(currentOffset))){currentOffset++;}
						betweenBlocks.add(new CustomToken(whiteSpaceStart, currentOffset, TokenType.WHITE_SPACE));
					}
					else if( currentChar == '#' )	// line comment
					{
						int commentStart = currentOffset;
						while(buffer.charAt(currentOffset) != '\n'){currentOffset++;}
						betweenBlocks.add(new CustomToken(commentStart, currentOffset, PERL_COMMENT));
					}
					else
						break;
				}

				// read block
				secondBlockOpener = new CustomToken(currentOffset, currentOffset+1, PERL_REGEX_QUOTE);
				secondBLock = RegexBlock.parseBlock(buffer, currentOffset + 1, bufferEnd, buffer.charAt(currentOffset));
			}

			if( secondBLock == null )
			{
//				System.err.println("Stop after second block");
				yybegin(YYINITIAL);
				return PERL_REGEX_QUOTE;
			}
			currentOffset = secondBLock.getEndOffset();
		}

		// check modifiers for x
		boolean isExtended = false;
		List<Character> allowedModifiers = RegexBlock.allowedModifiers.get(regexCommand);
		int modifiersEnd = currentOffset;
		ArrayList<CustomToken> modifierTokens = new ArrayList<CustomToken>();

		while(true)
		{
			if( modifiersEnd == bufferEnd)	// eof
				break;
			else if( !allowedModifiers.contains(buffer.charAt(modifiersEnd)))	// unknown modifier
				break;
			else if( buffer.charAt(modifiersEnd) == 'x')	// mark as extended
				isExtended = true;

			modifierTokens.add(new CustomToken(modifiersEnd, modifiersEnd + 1, PERL_REGEX_MODIFIER));

			modifiersEnd++;
		}

		// parse block 1
		tokensList.addAll(firstBlock.tokenize(isExtended));

		if( secondBLock != null )
		{
			// parse spaces
			tokensList.addAll(betweenBlocks);

			if( secondBlockOpener != null)
				tokensList.add(secondBlockOpener);

			// parse block 2
			tokensList.addAll(secondBLock.tokenize(isExtended));
		}

		// parse modifiers
		tokensList.addAll(modifierTokens);

		yybegin(LEX_PREPARSED_ITEMS);

		return PERL_REGEX_QUOTE;
	}


	/**
	 *	Transliteration processors tr y
	 **/

	public IElementType processTransOpener()
	{
		allowSharp = true;
		isEscaped = false;
		currentSectionNumber = 0;
		pushState();
		yybegin(LEX_TRANS_OPENER);
		return PERL_KEYWORD;
	}

	public IElementType processTransQuote()
	{
		charOpener = yytext().charAt(0);

		if( charOpener == '#' && !allowSharp )
		{
			yypushback(1);
			popState();
			return null;
		}
		else charCloser = RegexBlock.getQuoteCloseChar(charOpener);

		yybegin(LEX_TRANS_CHARS);
		stringContentStart = getTokenStart() + 1;

		return PERL_REGEX_QUOTE;
	}

	public IElementType processTransChar()
	{
		char currentChar = yytext().charAt(0);

		if( currentChar == charCloser && !isEscaped )
		{
			yypushback(1);
			setTokenStart(stringContentStart);
			yybegin(LEX_TRANS_CLOSER);
			return PERL_STRING_CONTENT;
		}
		else if( isLastToken() )
		{
			setTokenStart(stringContentStart);
			return PERL_STRING_CONTENT;
		}
		else
			isEscaped = ( currentChar == '\\' && !isEscaped );

		return null;
	}

	public IElementType processTransCloser()
	{
		if( currentSectionNumber == 0 ) // first section
		{
			currentSectionNumber++;
			if( charCloser == charOpener ) // next is replacements block
			{
				yybegin(LEX_TRANS_CHARS);
				stringContentStart = getTokenStart() + 1;
			}
			else	// next is new opener, possibly other
			{
				yybegin(LEX_TRANS_OPENER);
			}
		}
		else // last section
		{
			yybegin(LEX_TRANS_MODIFIERS);
		}
		return PERL_REGEX_QUOTE;
	}



	/**
	 *  Quote-like string procesors
	 **/
	public IElementType processQuoteLikeStringOpener()
	{
		allowSharp = true;
		isEscaped = false;
		pushState();
		yybegin(LEX_QUOTE_LIKE_OPENER);
		return PERL_KEYWORD;
	}

	public IElementType processQuoteLikeQuote()
	{
		charOpener = yytext().charAt(0);

		if( charOpener == '#' && !allowSharp )
		{
			yypushback(1);
			yybegin(YYINITIAL);
			return null;
		}
		else charCloser = RegexBlock.getQuoteCloseChar(charOpener);

		yybegin(LEX_QUOTE_LIKE_CHARS);
		stringContentStart = getTokenStart() + 1;

		return PERL_QUOTE;
	}

	public IElementType processQuoteLikeChar()
	{
		char currentChar = yytext().charAt(0);

		if( currentChar == charCloser && !isEscaped )
		{
			yypushback(1);
			setTokenStart(stringContentStart);
			yybegin(LEX_QUOTE_LIKE_CLOSER);
			return PERL_STRING_CONTENT;
		}
		else if( isLastToken() )
		{
			setTokenStart(stringContentStart);
			return PERL_STRING_CONTENT;
		}
		else
			isEscaped = ( currentChar == '\\' && !isEscaped );

		{
		}

		return null;
	}

	/**
	 *  Strings handler
	 */
	public IElementType processStringOpener()
	{
		isEscaped = false;
		charOpener = charCloser = yytext().charAt(0);
		stringContentStart = getTokenStart() + 1;
		pushState();
		yybegin(LEX_QUOTE_LIKE_CHARS);
		return PERL_QUOTE;
	}

	/**
	 *  Quote-like list procesors
	 **/

	public IElementType processQuoteLikeListOpener()
	{
		allowSharp = true;
		pushState();
		yybegin(LEX_QUOTE_LIKE_LIST_OPENER);
		return PERL_KEYWORD;
	}

	public IElementType processQuoteLikeListQuote()
	{
		charOpener = yytext().charAt(0);

		if( charOpener == '#' && !allowSharp )
		{
			yypushback(1);
			yybegin(YYINITIAL);
			return null;
		}
		else charCloser = RegexBlock.getQuoteCloseChar(charOpener);

		yybegin(LEX_QUOTE_LIKE_WORDS);

		return PERL_QUOTE;
	}


	public IElementType processQuoteLikeWord()
	{
		CharSequence currentToken = yytext();

		isEscaped = false;

		for( int i = 0; i < currentToken.length(); i++ )
		{
			if( !isEscaped && currentToken.charAt(i) == charCloser )
			{
				yypushback(currentToken.length() - i);
				yybegin(LEX_QUOTE_LIKE_LIST_CLOSER);

				return i == 0 ? null: PERL_STRING_CONTENT;
			}

			isEscaped = !isEscaped && currentToken.charAt(i) == '\\';
		}
		return PERL_STRING_CONTENT;
	}


	/**
	 *  Data block related code
	 */
	public int dataBlockStart = 0;

	public void processDataOpener()
	{
		dataBlockStart = getTokenStart();
		yybegin(LEX_EOF);
	}

	public IElementType endDataBlock()
	{
		setTokenStart(dataBlockStart);
		return PERL_COMMENT_BLOCK;
	}


	/**
	 *  Pod block-related code
	 */
	public IElementType capturePodBlock()
	{
		int podBlockStart = getTokenStart();
		CharSequence buffer = getBuffer();

		if( podBlockStart == 0 || buffer.charAt(podBlockStart-1) == '\n' || buffer.charAt(podBlockStart-1) == '\r' )
		{
			// pod block
			pushState();
			tokensList.clear();

			int bufferEnd = buffer.length();

			int currentPosition = podBlockStart;
			int linePos = currentPosition;

			while( true )
			{
				while(linePos < bufferEnd && buffer.charAt(linePos) != '\n' && buffer.charAt(linePos) != '\r'){linePos++;}

				int textEnd = linePos;

				while(linePos < bufferEnd && (buffer.charAt(linePos) == '\n' || buffer.charAt(linePos) == '\r')){linePos++;}

				String line = buffer.subSequence(currentPosition, textEnd).toString();

				currentPosition = linePos;

				if( linePos == bufferEnd || line.startsWith("=cut"))
				{
					tokensList.add(new CustomToken(podBlockStart, linePos, PERL_POD));
					yybegin(LEX_PREPARSED_ITEMS);
					return null;
				}
			}
		}

		yypushback(yylength() - 1);
		return PERL_OPERATOR;
	}

	/** contains marker for multiline end **/
	public String heredocMarker;

	public Pattern markerPattern = Pattern.compile("<<\\s*['\"`]?([^\"\'`]+)['\"`]?");

	/**
	 * Invoken on opening token, waiting for a newline
	 */
	public IElementType processMultilineOpener()
	{
		String openToken = yytext().toString();
		Matcher m = markerPattern.matcher(openToken);
		if (m.matches())
		{
			heredocMarker = m.group(1);
		}

		pushState();
		yybegin(LEX_MULTILINE_WAITING);
		yypushback(openToken.length() - 2);

		return PERL_OPERATOR;
	}

	public boolean waitingMultiline(){return yystate() == LEX_MULTILINE_WAITING;}

	public IElementType processSemicolon()
	{
		if( !waitingMultiline() )
			yybegin(YYINITIAL);
		else
		{
			stateStack.pop();
			stateStack.push(YYINITIAL);
		}
		return PERL_SEMI;
	}

	public void captureMultiline()
	{
		tokensList.clear();
		tokensList.add(new CustomToken(getTokenStart(), getTokenEnd(), TokenType.NEW_LINE_INDENT));

		int currentPosition = getTokenEnd();
		int stringStart = currentPosition;

		CharSequence buffer = getBuffer();
		int bufferEnd = buffer.length();

		while( true )
		{
			int lineStart = currentPosition;
			int linePos = currentPosition;

			while(linePos < bufferEnd && buffer.charAt(linePos) != '\n' && buffer.charAt(linePos) != '\r'){linePos++;}

			int textEnd = linePos;

			while(linePos < bufferEnd && (buffer.charAt(linePos) == '\n' || buffer.charAt(linePos) == '\r')){linePos++;}

			int lineEnd = linePos;

			String line = buffer.subSequence(lineStart, textEnd).toString();

			if( heredocMarker.equals(line))
			{
				tokensList.add(new CustomToken(stringStart, lineStart, PERL_STRING_MULTILINE));
				tokensList.add(new CustomToken(lineStart, textEnd, PERL_STRING_MULTILINE_END));
				yybegin(LEX_PREPARSED_ITEMS);
				yypushback(1);
				break;
			}
			else if(lineEnd == bufferEnd)
			{
				tokensList.add(new CustomToken(stringStart, lineEnd, PERL_STRING_MULTILINE));
				yybegin(LEX_PREPARSED_ITEMS);
				yypushback(1);
				break;
			}
			else
				currentPosition = lineEnd;
		}

	}

	public IElementType processNewLine()
	{
		if( waitingMultiline() )
			captureMultiline();
		return TokenType.NEW_LINE_INDENT;
	}

}
