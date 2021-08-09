import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sh_translator/sh_translator.dart';

void main() {
  const MethodChannel channel = MethodChannel('sh_translator');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await ShTranslator.platformVersion, '42');
  });
}
