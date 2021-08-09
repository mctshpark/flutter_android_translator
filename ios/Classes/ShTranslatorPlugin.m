#import "ShTranslatorPlugin.h"
#if __has_include(<sh_translator/sh_translator-Swift.h>)
#import <sh_translator/sh_translator-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "sh_translator-Swift.h"
#endif

@implementation ShTranslatorPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftShTranslatorPlugin registerWithRegistrar:registrar];
}
@end
