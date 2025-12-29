package com.nowwhat.app.utils

import android.content.Context
import com.nowwhat.app.R
import com.nowwhat.app.model.AppLanguage
import com.nowwhat.app.model.Gender

object StringUtils {

    fun getGenderString(context: Context, gender: Gender): String {
        return when (gender) {
            Gender.Male -> context.getString(R.string.gender_male)
            Gender.Female -> context.getString(R.string.gender_female)
            Gender.NotSpecified -> context.getString(R.string.gender_not_specified)
        }
    }

    fun getLanguageDisplayName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.English -> "English"
            AppLanguage.Hebrew -> "עברית"
            AppLanguage.Russian -> "Русский"
        }
    }
}