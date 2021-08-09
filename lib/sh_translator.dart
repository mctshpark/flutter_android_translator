import 'dart:async';

import 'package:flutter/services.dart';

class ShTranslator {
  static const MethodChannel _channel = const MethodChannel('sh_translator');

  ///번역 시도 (언어팩이 없는 경우 언어팩을 다운로드 받음 언어팩당 약 30MB)
  ///* argument
  /// text : 번역 할 텍스트
  /// targetCode : 번역 될 언어 코드
  static Future<String> autoTranslate({String text, String targetCode}) async {
    if (text == null || text.length == 0) return Future.value('');
    if (targetCode == null || targetCode.length == 0) return Future.value('');

    return await _channel.invokeMethod('auto_translate', <String, dynamic>{
      'text': text,
      'targetCode': targetCode,
    });
  }

  static Future<String> translate({String text, String targetCode, String sourceCode}) async {
    assert(text != null && text != "" && targetCode != null && targetCode != "" && sourceCode != null && sourceCode != "");
    return await _channel.invokeMethod('translate', <String, dynamic>{
      'text': text,
      'sourceCode': sourceCode,
      'targetCode': targetCode,
    });
  }

  ///유효한 언어팩 목록 가져오기
  static Future<dynamic> getAllLanguage() async {
    return _channel.invokeMethod('get_all_language');
  }

  ///다운로드 완료한 언어팩 목록 가져오기
  static Future<dynamic> checkLanguage() async {
    return _channel.invokeMethod('check_language');
  }

  ///로컬에 다운로드 된 언어팩 제거
  ///* argument
  /// deleteModel : 삭제할 언어팩 언어코드
  static Future<bool> deleteLanguageModel(String deleteModel) async {
    return await _channel.invokeMethod('delete_language_model', <String, dynamic>{'delete_model': deleteModel});
  }

  static Future<String> getDisplayLanguage(String languageCode) async {
    return await _channel.invokeMethod('get_display_language', <String, dynamic>{'language_code': languageCode});
  }
}
