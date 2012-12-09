//
//  TestViewController.h
//  Rsc Demo
//
//  Created by jrichards on 10/16/11.
//  Copyright 2011 SETI INSTITUTE. All rights reserved.
//

#import <UIKit/UIKit.h>

//Define the protocol for the delegate
@protocol TestViewControllerDelegate
- (void)startLoopbackTestCallBack;
- (void)startStopTxTest:(BOOL)start;
- (void)startStopRxTest:(BOOL)start;
- (void)sendTextCallBack:(NSString *)s;
- (void)testingViewFinished;
@end

@interface TestViewController : UIViewController {
    
    UITextField *txText;
    UITextView  *rxText;
    UIButton    *loopTestButton;
    UIButton    *txTestButton;
    UIButton    *rxTestButton;

    
    id <TestViewControllerDelegate> delegate;
}

@property (nonatomic, retain) IBOutlet UITextField *txText;
@property (nonatomic, retain) IBOutlet UITextView  *rxText;
@property (nonatomic, retain) IBOutlet UIButton    *loopTestButton;
@property (nonatomic, retain) IBOutlet UIButton    *txTestButton;
@property (nonatomic, retain) IBOutlet UIButton    *rxTestButton;

- (IBAction)loopTestButtonPressed:(id)sender;
- (IBAction)sendTextEntered:(id)sender;
- (IBAction)txTestButtonPressed:(id)sender;
- (IBAction)rxTestButtonPressed:(id)sender;

- (void) reset;

@property (nonatomic, assign) id  <TestViewControllerDelegate> delegate; 

@end
