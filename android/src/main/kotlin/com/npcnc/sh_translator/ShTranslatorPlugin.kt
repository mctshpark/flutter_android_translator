package com.npcnc.sh_translator

import android.util.Log
import android.util.LruCache
import androidx.annotation.NonNull
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.google.mlkit.nl.translate.*
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** ShTranslatorPlugin */
class ShTranslatorPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity

    companion object {
        // This specifies the number of translators instance we want to keep in our LRU cache.
        // Each instance of the translator is built with different options based on the source
        // language and the target language, and since we want to be able to manage the number of
        // translator instances to keep around, an LRU cache is an easy way to achieve this.
        private const val NUM_TRANSLATORS = 3
    }

    private lateinit var channel: MethodChannel
    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()
    private val translators =
            object : LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
                override fun create(options: TranslatorOptions): Translator {
                    return Translation.getClient(options)
                }

                override fun entryRemoved(
                        evicted: Boolean,
                        key: TranslatorOptions,
                        oldValue: Translator,
                        newValue: Translator?
                ) {
                    oldValue.close()
                }
            }
    val sourceLang = MutableLiveData<Language>()
    val targetLang = MutableLiveData<Language>()
    val sourceText = MutableLiveData<String>()
    val translatedText = MediatorLiveData<ResultOrError>()
    val availableModels = MutableLiveData<List<String>>()

    // Gets a list of all available translation languages.
    val availableLanguages: List<Language> = TranslateLanguage.getAllLanguages()
            .map {
                Language(it)
            }

    private fun getModel(languageCode: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(languageCode).build()
    }

    // Updates the list of downloaded models available for local translation.
    private fun fetchDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { remoteModels ->
                    availableModels.value =
                            remoteModels.sortedBy { it.language }.map { it.language }
                }
    }

    // Starts downloading a remote model for local translation.
    internal fun downloadLanguage(language: Language) {
        val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
        modelManager.download(model, DownloadConditions.Builder().build())
                .addOnCompleteListener { fetchDownloadedModels() }
    }

    // Deletes a locally stored translation model.
    internal fun deleteLanguage(deleteModel: String) {
        val model = getModel(TranslateLanguage.fromLanguageTag(deleteModel)!!)
        modelManager.deleteDownloadedModel(model).addOnCompleteListener { fetchDownloadedModels() }
    }

    private fun autoTranslate(@NonNull call: MethodCall, @NonNull result: Result) {
        val targetCode = call.argument<String>("targetCode") ?: ""
        val textString = call.argument<String>("text") ?: ""
        var languageCode = ""
        //언어 인식
        val languageIdentifier = LanguageIdentification.getClient(
                LanguageIdentificationOptions
                        .Builder()
                        .setConfidenceThreshold(0.34f)
                        .build()
        )

        languageIdentifier.identifyPossibleLanguages(textString)
                .addOnSuccessListener { identifiedLanguages ->
                    for (identifiedLanguage in identifiedLanguages) {
                        languageCode = identifiedLanguage.languageTag
                        val confidence = identifiedLanguage.confidence
                        Log.i("로그", "언어 식별 정보 $languageCode $confidence")
                    }

                    if (TranslateLanguage.getAllLanguages().contains(languageCode)) {
                        //식별한 언어가 번역 지원언어에 포함 되어있음
                        Log.i("로그", "선택된 언어 $languageCode")
                        val targetLangCode = TranslateLanguage.fromLanguageTag(targetCode)!!
                        val options = TranslatorOptions.Builder()
                                .setSourceLanguage(languageCode)
                                .setTargetLanguage(targetLangCode)
                                .build()


                        val resultText = translators[options].downloadModelIfNeeded().continueWithTask { task ->
                            if (task.isSuccessful) {
                                translators[options].translate(textString)
                            } else {
                                Log.i("로그", "자동번역 언어팩 다운로드 또는 번역 실패")
                                Tasks.forException<String>(
                                        task.exception
                                                ?: Exception("Unknown error occurred.")
                                )
                            }
                        }

                        resultText.addOnCompleteListener {
                            try {
                                result.success("${it.result}")
                            } catch (e: Exception) {
                                Log.i("로그", "완료된 번역 처리 에러 ${e.cause}")
                                Log.i("로그", "완료된 번역 처리 에러 ${e.message}")
                                Log.i("로그", "완료된 번역 처리 에러 ${e.localizedMessage}")
                                Log.i("로그", "완료된 번역 처리 에러 ${e.stackTrace}")
                                result.error(
                                        "1002",
                                        "완료된 번역처리 에러\n" +
                                                "${e.cause}",
                                        "완료된 번역처리 에러\n" +
                                                "${e.message}"
                                )
                            }
                        }

                        resultText.addOnFailureListener {
                            result.error("1003", "마지막 번역작업 실패${it.message}", "${it.cause}")
                        }
                    } else {
                        //식별한 언어가 번역 지원언어에 포함되어 있지 않음
                        result.error(
                                "1001",
                                Locale(languageCode).displayLanguage,
                                "인식한 언어코드[${languageCode}]는 번역 지원언어에 포함되지 않는 언어입니다."
                        )
                    }

                }
                .addOnFailureListener {
                    Log.i("로그", "자동번역 언어 인식 실패")
                    result.error("1000", "언어 인식 실패 ${it.message}", "언어 인식에 실패하였습니다.${it.cause}")
                }

    }

    private fun translate(@NonNull call: MethodCall, @NonNull result: Result) {
        val sourceCode = call.argument<String>("sourceCode") ?: ""
        val targetCode = call.argument<String>("targetCode") ?: ""
        val textString = call.argument<String>("text") ?: ""

        val targetLangCode = TranslateLanguage.fromLanguageTag(targetCode)!!
        val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceCode)
                .setTargetLanguage(targetLangCode)
                .build()

        val resultText = translators[options].downloadModelIfNeeded().continueWithTask { task ->
            if (task.isSuccessful) {
                translators[options].translate(textString)
            } else {
                Log.i("로그", "언어 사전선택 번역 언어팩 다운로드 실패 또는 번역 오류 Unknown error occurred.")
                Tasks.forException<String>(
                        task.exception
                                ?: Exception("Unknown error occurred.")
                )
            }
        }

        resultText.addOnCompleteListener {
            try {
                result.success("${it.result}")
            } catch (e: Exception) {
                Log.i("로그", "언어 사전선택 번역 완료 처리 에러 ${e.cause}")
            }
        }
        resultText.addOnFailureListener {
            result.error("언어 사전선택 최종 번역 실패", "${it.message}", "${it.cause}")
        }
    }


    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sh_translator")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.v("로그", "[Call ShTranslate] ${call.method} | arguments : ${call.arguments}")

        when (call.method) {
            "auto_translate" -> {
                autoTranslate(call, result)
            }
            "translate" -> {
                translate(call, result)
            }

            "check_language" -> {
                val modelList: ArrayList<HashMap<String, String>> = arrayListOf()

                modelManager.getDownloadedModels(TranslateRemoteModel::class.java).addOnCompleteListener {
                    it.result?.forEach {
                        modelList.add(Language(it.language).toMap())
                    }
                    result.success(modelList)

                }.addOnFailureListener {
                    Log.i("로그", "다운완료된 언어팩 목록 가져오기 실패 ${it.cause}")
                }
            }

            "delete_language_model" -> {
                val deleteModel = call.argument<String>("delete_model") ?: ""
                val model = getModel(TranslateLanguage.fromLanguageTag(deleteModel)!!)
                modelManager.deleteDownloadedModel(model).addOnCompleteListener {
                    fetchDownloadedModels()
                    result.success(true)
                }.addOnFailureListener {
                    result.success(false)
                }
            }

            "get_all_language" -> {
                val modelList: ArrayList<HashMap<String, String>> = arrayListOf()
                TranslateLanguage.getAllLanguages().forEach {
                    modelList.add(Language(it).toMap())
                }

                result.success(modelList)
            }

            "get_display_language" -> {
                val flutterLanguageCode = call.argument<String>("language_code") ?: ""
                result.success(Locale(flutterLanguageCode).displayLanguage)
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    /**
     * Holds the result of the translation or any error.
     */
    inner class ResultOrError(var result: String?, var error: Exception?)

    /**
     * holds the language code (i.e. "en") and the corresponding localized full language name
     * (i.e. "English")
     */
    class Language(val code: String) : Comparable<Language> {

        private val displayName: String
            get() = Locale(code).displayName

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }

            if (other !is Language) {
                return false
            }

            val otherLang = other as Language?
            return otherLang!!.code == code
        }


        fun toMap(): HashMap<String, String> {
            val map: HashMap<String, String> = HashMap()
            map[code] = displayName
            return map
        }

        override fun toString(): String {
            return "$code - $displayName"
        }

        override fun compareTo(other: Language): Int {
            return this.displayName.compareTo(other.displayName)
        }

        override fun hashCode(): Int {
            return code.hashCode()
        }
    }
}
