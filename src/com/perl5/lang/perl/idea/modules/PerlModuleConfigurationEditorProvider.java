/*
 * Copyright 2015 Alexandr Evstigneev
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

package com.perl5.lang.perl.idea.modules;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ui.configuration.*;

/**
 * Created by hurricup on 07.06.2015.
 */
public class PerlModuleConfigurationEditorProvider implements ModuleConfigurationEditorProvider
{
	@Override
	public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state)
	{
		Module module = state.getRootModel().getModule();
		if (ModuleType.get(module) instanceof PerlModuleType) {
			return new ModuleConfigurationEditor[]{
					new ContentEntriesEditor(module.getName(), state)
			};
		}
		return ModuleConfigurationEditor.EMPTY;
	}
}
