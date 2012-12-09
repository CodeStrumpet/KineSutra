//
//  Rsc_DemoAppDelegate.h
//  Rsc Demo
//
//  Copyright © 2011 Redpark  All Rights Reserved
//

#import <UIKit/UIKit.h>

@interface Rsc_DemoAppDelegate : NSObject <UIApplicationDelegate> {
    
    UIWindow *window;
    UINavigationController *navigationController;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet UINavigationController *navigationController;

@end

