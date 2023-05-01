package com.grit.slutprojekt_turist

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions


import com.grit.slutprojekt_turist.databinding.ActivityTranslatorBinding

import java.util.*
import kotlin.collections.ArrayList

class TranslatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTranslatorBinding

    companion object{
        private const val TAG = "MAIN_TAG"
    }


    private var languageArrayList: ArrayList<ModelLanguage>? = null

    // Defaults
    private var sourceLanguageCode = "en"
    private var sourceLanguageTitle = "English"
    private var targetLanguageCode = "se"
    private var targetLanguageTitle = "Swedish"

    private val PREFS_NAME = "translator_prefs"
    private val SRC_LANG_TEXT_KEY = "src_lang_text"
    private val TARGET_LANG_TEXT_KEY = "target_lang_text"

    private lateinit var translatorOptions: TranslatorOptions

    private lateinit var translator: Translator

    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            sourceLanguageTitle = savedInstanceState.getString("sourceLanguageTitle", "")
            targetLanguageTitle = savedInstanceState.getString("targetLanguageTitle", "")
        }


        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        loadAvailableLanguage()

        binding.srcLangChooseBtn.setOnClickListener{
            sourceLanguageChooser()
        }

        binding.targetLangChooserBtn.setOnClickListener{
            targetLanguageChooser()
        }

        binding.translateBtn.setOnClickListener{
            validateData()
        }

        binding.homeBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private var sourceLanguageText = ""

    private fun validateData() {

        sourceLanguageText = binding.srcLangEt.text.toString().trim()

        Log.d(TAG, "validateData: sourceLanguageText: $sourceLanguageText")

        if (sourceLanguageText.isEmpty()){
            showToast("Enter text to translate")

        } else {
            startTranslation()
        }
    }

    private fun startTranslation() {

        progressDialog.setMessage("Processing language model...")
        progressDialog.show()

        Log.d(TAG, "startTranslation: setSourceLanguage: $sourceLanguageCode")
        Log.d(TAG, "startTranslation: setTargetLanguage: $targetLanguageCode")

        // I think the error is coming from here
        // I tried:
        /*
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLanguageCode).toString())
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLanguageCode).toString())

            Thinking setSourceLanguage() only can take full Languge names such as "SWEDEN"
         */

        val sourceLanguageCodeISO = Locale(sourceLanguageCode).language
        val targetLanguageCodeISO = Locale(targetLanguageCode).language

        translatorOptions = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguageCodeISO)
            .setTargetLanguage(targetLanguageCodeISO)
            .build()

        translator = Translation.getClient(translatorOptions)

        val downloadConditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(downloadConditions)
            .addOnSuccessListener {

                Log.d(TAG, "startTranslation: model ready, start translation...")

                progressDialog.setMessage("Translating...")

                translator.translate(sourceLanguageText)
                    .addOnSuccessListener { translatedText ->

                        Log.d(TAG, "startTranslation: translatedText: $translatedText")

                        progressDialog.dismiss()

                        binding.targetLangTv.text = translatedText

                    }
                    .addOnFailureListener{ e ->

                        progressDialog.dismiss()

                        Log.d(TAG, "startTranslation: ", e)

                        showToast("Failed to translate due to ${e.message}")

                    }
            }
            .addOnFailureListener { e ->
                
                progressDialog.dismiss()
                Log.d(TAG, "startTranslation: ", e)

                showToast("Failed due to ${e.message}")
            }
    }

    private fun loadAvailableLanguage(){

        languageArrayList = ArrayList()

        val languageCodeList = TranslateLanguage.getAllLanguages()

        for (languageCode in languageCodeList){

            val languageTitle = Locale(languageCode).displayLanguage

            Log.d(TAG, "loadAvailableLanguage: languageCode: $languageCode")
            Log.d(TAG, "loadAvailableLanguage: languageTitle: $languageTitle")

            val modelLanguage = ModelLanguage(languageCode, languageTitle)

            languageArrayList!!.add(modelLanguage)
        }
    }

    private fun sourceLanguageChooser(){

        val popupMenu = PopupMenu(this, binding.srcLangChooseBtn)

        for (i in languageArrayList!!.indices){

            popupMenu.menu.add(Menu.NONE, i,i, languageArrayList!![i].languageTitle)
        }

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->

            val position = menuItem.itemId

            sourceLanguageCode = languageArrayList!![position].languageCode
            sourceLanguageTitle = languageArrayList!![position].languageTitle

            binding.srcLangChooseBtn.text = sourceLanguageTitle
            binding.srcLangEt.hint = "Enter $sourceLanguageTitle"

            Log.d(TAG, "sourceLanguageChooser: sourceLanguageCode: $sourceLanguageCode ")
            Log.d(TAG, "sourceLanguageChooser: sourceLanguageTitle: $sourceLanguageTitle ")

            false
        }
    }

    private fun targetLanguageChooser() {

        val popupMenu = PopupMenu(this, binding.targetLangChooserBtn)

        for (i in languageArrayList!!.indices){

            popupMenu.menu.add(Menu.NONE, i,i, languageArrayList!![i].languageTitle)
        }

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->

            val position = menuItem.itemId

            targetLanguageCode = languageArrayList!![position].languageCode
            targetLanguageTitle = languageArrayList!![position].languageTitle

            binding.targetLangChooserBtn.text = targetLanguageTitle

            Log.d(TAG, "targetLanguageChooser: targetLanguageCode $targetLanguageCode")
            Log.d(TAG, "targetLanguageChooser: targetLanguageTitle $targetLanguageTitle")

            false
        }

    }

    private fun showToast(message: String){
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onPause() {
        super.onPause()

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putString(SRC_LANG_TEXT_KEY, binding.srcLangEt.text.toString())
        editor.putString(TARGET_LANG_TEXT_KEY, binding.targetLangTv.text.toString())

        editor.apply()
    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        binding.srcLangEt.setText(sharedPreferences.getString(SRC_LANG_TEXT_KEY, ""))
        binding.targetLangTv.text = sharedPreferences.getString(TARGET_LANG_TEXT_KEY, "")
    }
}