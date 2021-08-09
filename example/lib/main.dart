import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import 'package:sh_translator/sh_translator.dart';

const languageList = [
  'af', //아프리칸스어
  'sq', //알바니아어
  'ar', //아랍어
  'be', //벨라루스어
  'bg', //불가리아어
  'bn', //벵골어
  'ca', //카탈로니아어
  'zh', //중국어
  'hr', //크로아티아어
  'cs', //체코어
  'da', //덴마크어
  'nl', //네덜란드어
  'en', //영어
  'eo', //에스페란토어
  'et', //에스토니아어
  'fi', //핀란드어
  'fr', //프랑스어
  'gl', //갈리시아어
  'ka', //조지아어
  'de', //독일어
  'el', //그리스어
  'gu', //구자라트어
  'ht', //아이티어
  'he', //히브리어
  'hi', //힌디어
  'hu', //헝가리어
  'is', //아이슬란드어
  'id', //인도네시아어
  'ga', //아일랜드어
  'it', //이탈리아어
  'ja', //일본어
  'kn', //칸나다어
  'ko', //한국어
  'lt', //리투아니아어
  'lv', //라트비아어
  'mk', //마케도니아어
  'mr', //마라티어
  'ms', //말레이어
  'mt', //몰타어
  'no', //노르웨이어
  'fa', //페르시아어
  'pl', //폴란드어
  'pt', //포르투갈어
  'ro', //루마니아어
  'ru', //러시아어
  'sk', //슬로바키아어
  'sl', //슬로베니아어
  'es', //스페인어
  'sv', //스웨덴어
  'sw', //스와힐리어
  'tl', //타갈로그어
  'ta', //타밀어
  'te', //텔루구어
  'th', //태국어
  'tr', //터키어
  'uk', //우크라이나어
  'ur', //우르두어
  'vi', //베트남어
  'cy', //웨일스어
];

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  TextEditingController tc;
  RxString nativeLanguage = '한국어 입니다.'.obs;
  RxString translatedLanguage = ''.obs;
  RxBool loading = false.obs;
  String targetLocale = 'en';

  @override
  void initState() {
    super.initState();
    tc = TextEditingController(text: nativeLanguage.value);
    initPlatformState();
    debounce(nativeLanguage, (callback) => translate(), time: Duration(milliseconds: 800));
  }

  Future<void> translate() async {
    loading.value = true;
    translatedLanguage.value = await ShTranslator.autoTranslate(text: nativeLanguage.value, targetCode: targetLocale);
    loading.value = false;
    print(translatedLanguage.value);
  }

  Future<void> initPlatformState() async {
    try {
      loading.value = true;
      translatedLanguage.value = await ShTranslator.autoTranslate(text: nativeLanguage.value, targetCode: targetLocale);
      loading.value = false;
      //List<dynamic> langList = await ShTranslator.getAllLanguage();
      //print('${await ShTranslator.getAllLanguage()}');
      print('${await ShTranslator.getDisplayLanguage(targetLocale)}');
    } on PlatformException catch (e) {
      print('ERROR Code ${e.code}');
      print('ERROR Message ${e.message}');
      print('ERROR Details ${e.details}');
    }
  }

  @override
  void dispose() {
    tc.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Plugin example app')),
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              margin: EdgeInsets.only(top: 15, left: 15),
              child: Text('한국어'),
            ),
            Container(
              margin: EdgeInsets.all(15),
              child: TextField(
                controller: tc,
                decoration: InputDecoration(
                  filled: true,
                  contentPadding: EdgeInsets.only(left: 15, right: 15, top: 8, bottom: 8),
                  focusedBorder: const OutlineInputBorder(),
                  enabledBorder: const OutlineInputBorder(),
                  border: const OutlineInputBorder(),
                  hintText: '번역할 내용을 입력하세요',
                  hintStyle: TextStyle(
                    color: Colors.grey,
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                onChanged: (value) => nativeLanguage.value = value,
                onSubmitted: (value) {},
              ),
            ),
            Container(
              margin: EdgeInsets.only(top: 15, left: 15),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Text('영어'),
                  SizedBox(width: 10),
                  Obx(
                    () => Visibility(
                      visible: loading.value,
                      child: SizedBox(
                        width: 15,
                        height: 15,
                        child: CircularProgressIndicator(strokeWidth: 1.5),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Flexible(
              child: Obx(
                () => Container(
                  width: double.infinity,
                  margin: EdgeInsets.all(15),
                  decoration: BoxDecoration(border: Border.all(), borderRadius: BorderRadius.all(Radius.circular(8))),
                  padding: EdgeInsets.only(left: 15, right: 15, top: 8, bottom: 8),
                  child: Text(translatedLanguage.value),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
