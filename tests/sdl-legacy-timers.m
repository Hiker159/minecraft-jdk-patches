/* SPDX-License-Identifier: GPL-2.0-only */
#import <Foundation/Foundation.h>
#include <assert.h>
#include <dlfcn.h>
@protocol LegacyTimerFactory
+ (NSTimer *)timerWithTimeInterval:(NSTimeInterval)interval repeats:(BOOL)repeats block:(void (^)(NSTimer *))block;
+ (NSTimer *)scheduledTimerWithTimeInterval:(NSTimeInterval)interval repeats:(BOOL)repeats block:(void (^)(NSTimer *))block;
@end
int main(int argc, char **argv) {
    @autoreleasepool {
        assert(argc == 2);
        assert(dlopen(argv[1], RTLD_NOW | RTLD_LOCAL) != NULL);
        Class<LegacyTimerFactory> factory = NSClassFromString(@"SDL3MavericksTimer");
        assert(factory != Nil);
        __block int once = 0, repeated = 0;
        NSTimer *one = [factory timerWithTimeInterval:0.01 repeats:NO block:^(NSTimer *timer) { once++; }];
        [[NSRunLoop currentRunLoop] addTimer:one forMode:NSDefaultRunLoopMode];
        NSTimer *many = [factory scheduledTimerWithTimeInterval:0.01 repeats:YES block:^(NSTimer *timer) {
            repeated++;
            if (repeated == 3) [timer invalidate];
        }];
        NSDate *limit = [NSDate dateWithTimeIntervalSinceNow:1.0];
        while ([limit timeIntervalSinceNow] > 0 && (once == 0 || repeated < 3)) {
            [[NSRunLoop currentRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.02]];
        }
        [[NSRunLoop currentRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.05]];
        assert(once == 1 && repeated == 3 && !one.valid && !many.valid);
        puts("PASS: legacy SDL timers fire, repeat, and invalidate");
    }
}
