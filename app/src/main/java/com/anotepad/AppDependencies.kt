package com.anotepad

import com.anotepad.data.PreferencesRepository
import com.anotepad.data.TemplateRepository
import com.anotepad.file.FileRepository

class AppDependencies(
    val preferencesRepository: PreferencesRepository,
    val templateRepository: TemplateRepository,
    val fileRepository: FileRepository
)
