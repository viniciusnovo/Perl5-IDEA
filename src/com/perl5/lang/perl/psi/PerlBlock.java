/*
 * Copyright 2015-2017 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.perl5.lang.perl.psi.properties.PerlLexicalScope;
import com.perl5.lang.perl.psi.properties.PerlLoop;
import com.perl5.lang.perl.psi.utils.PerlPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.perl5.lang.perl.PerlParserDefinition.FILE;
import static com.perl5.lang.perl.lexer.PerlElementTypesGenerated.*;
import static com.perl5.lang.perl.lexer.PerlTokenSets.LAZY_CODE_BLOCKS;

/**
 * Created by hurricup on 04.03.2016.
 */
public interface PerlBlock extends PerlLoop, PerlLexicalScope {
  TokenSet LOOPS_CONTAINERS = TokenSet.create(
    BLOCK,
    FILE,
    WHILE_COMPOUND,
    UNTIL_COMPOUND,
    FOR_COMPOUND,
    FOREACH_COMPOUND,
    CONTINUE_BLOCK,
    NAMESPACE_DEFINITION,
    TRY_EXPR,
    CATCH_EXPR
  );
  TokenSet BLOCKS_WITH_RETURN_VALUE = TokenSet.create(
    SUB_EXPR,
    DO_EXPR,
    EVAL_EXPR,
    SUB_DEFINITION,
    METHOD_DEFINITION,
    FUNC_DEFINITION
  );

  @Nullable
  @Override
  default PsiPerlContinueBlock getContinueBlock() {
    PsiElement potentialBlock = PerlPsiUtil.getNextSignificantSibling(this);
    return potentialBlock instanceof PsiPerlContinueBlock ? (PsiPerlContinueBlock)potentialBlock : null;
  }

  /**
   * @return parent psi element for closest parent block element
   */
  @Nullable
  static PsiElement getClosestBlockContainer(@NotNull PsiElement position) {
    PsiPerlBlock enclosingBlock = PsiTreeUtil.getParentOfType(position, PsiPerlBlock.class);
    if (enclosingBlock == null) {
      return null;
    }
    PsiElement blockContainer = enclosingBlock.getParent();
    return LAZY_CODE_BLOCKS.contains(PsiUtilCore.getElementType(blockContainer))
           ? blockContainer.getParent() : blockContainer;
  }
}
