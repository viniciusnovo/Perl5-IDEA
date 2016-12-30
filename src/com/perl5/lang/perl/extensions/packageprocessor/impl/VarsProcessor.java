package com.perl5.lang.perl.extensions.packageprocessor.impl;

import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.perl5.lang.perl.extensions.packageprocessor.PerlPragmaProcessorBase;
import com.perl5.lang.perl.parser.PerlParserImpl;
import com.perl5.lang.perl.parser.PerlParserUtil;
import com.perl5.lang.perl.parser.builder.PerlBuilder;
import org.jetbrains.annotations.NotNull;

public class VarsProcessor extends PerlPragmaProcessorBase
{
	@Override
	public boolean parseUseParameters(@NotNull PerlBuilder b, int l, @NotNull GeneratedParserUtilBase.Parser defaultParser)
	{
		PerlParserUtil.passPackageAndVersion(b, l);
		b.setUseVarsContent(true);
		PerlParserImpl.expr(b, l, -1);
		b.setUseVarsContent(false);
		return true;
	}
}
