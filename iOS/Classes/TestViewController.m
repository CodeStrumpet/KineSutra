//
//  TestViewController.m
//  Rsc Demo
//
//  Created by jrichards on 10/16/11.
//  Copyright © 2011-2012 Redpark  All Rights Reserved
//

#import "TestViewController.h"
#import "RootViewController.h"


#define RX_TEST_TITLE @"Rx Test"
#define TX_TEST_TITLE @"Tx Test"
#define STOP_TITLE @"Stop"

@implementation TestViewController

@synthesize rxText;
@synthesize txText;
@synthesize loopTestButton;
@synthesize txTestButton;
@synthesize rxTestButton;
@synthesize delegate;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)dealloc
{
    [[self delegate] testingViewFinished];
    [super dealloc];
}

- (void)didReceiveMemoryWarning
{
    // Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
    
    // Release any cached data, images, etc that aren't in use.
}

#pragma mark - View lifecycle

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view from its nib.
    
    self.view.backgroundColor =  [UIColor colorWithRed:0.0 green:0.0 blue:0.0 alpha:1];
    
    self.navigationItem.hidesBackButton = NO;
    self.navigationController.navigationBar.backItem.title = @"Back";  

    [self reset];
}

- (void)viewDidUnload
{
    
    [super viewDidUnload];
    
    
    // Release any retained subviews of the main view.
    // e.g. self.myOutlet = nil;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    // Return YES for supported orientations
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}

- (void) reset
{
    [self.txTestButton setTitle:@"Tx Test" forState:UIControlStateNormal];
    [self.rxTestButton setTitle:@"Rx Test" forState:UIControlStateNormal];
    self.rxText.text = @""; 
}

- (IBAction)loopTestButtonPressed:(id)sender
{
    [[self delegate] startLoopbackTestCallBack];
}

- (IBAction)txTestButtonPressed:(id)sender
{
    UIButton *button = (UIButton *)sender;
    
    BOOL state = [button.titleLabel.text isEqualToString:STOP_TITLE];
    
    if (state)
    {
        [button setTitle:TX_TEST_TITLE forState:UIControlStateNormal];
    }
    else
    {
        [button setTitle:STOP_TITLE forState:UIControlStateNormal];
    }
    
    [self.delegate startStopTxTest:!state];
}

- (IBAction)rxTestButtonPressed:(id)sender
{
    UIButton *button = (UIButton *)sender;
    
    BOOL state = [button.titleLabel.text isEqualToString:STOP_TITLE];
    
    if (state)
    {
        [button setTitle:RX_TEST_TITLE forState:UIControlStateNormal];
    }
    else
    {
        [button setTitle:STOP_TITLE forState:UIControlStateNormal];
    }
    
    [self.delegate startStopRxTest:!state];

}

- (IBAction)sendTextEntered:(id)sender
{
    //NSMutableString *s1 = [[NSString stringWithFormat:@"%@\n", txText.text] retain];
    //[[self delegate] sendTextCallBack:s1];
    //[s1 release];
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {

    NSMutableString *s1 = [[NSString stringWithFormat:@"%@\r\n", txText.text] retain];
    [[self delegate] sendTextCallBack:s1];
    [s1 release];
    
    [txText resignFirstResponder]; //remove keyboard

    return YES;
}

@end
