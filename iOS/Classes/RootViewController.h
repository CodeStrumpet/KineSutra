//
//  RootViewController.h
//  Rsc Demo
//
//  Copyright © 2011 Redpark  All Rights Reserved
//

#import <UIKit/UIKit.h>
#import "SelectionViewController.h"
#import "TestViewController.h"
#import "RscMgr.h"

#define LOOPBACK_TEST_LEN 4096

typedef enum CableConnectState
{
	kCableNotConnected,
	kCableConnected,
	kCableRequiresPasscode

} CableConnectState;

typedef enum  
{
	kBaudIndex = 0,
	kDataBitsIndex = 1,
	kParityIndex = 2,
	kStopBitsIndex = 3
	
} PortConfigSettingType;



typedef enum
{
	kStatRx = 0,
	kStatTx = 1,
    kStatErr = 2
} StatType;

@interface RootViewController : UITableViewController < RscMgrDelegate, SelectionViewControllerDelegate, TestViewControllerDelegate> {
	UILabel *ctsLabel;
	UILabel *dsrLabel;
	UILabel *cdLabel;
	UILabel *riLabel;
	UIBarButtonItem *rtsButton;
	UIBarButtonItem *dtrButton;
	
	NSDictionary *portConfigTableData;
	NSArray *portConfigKeys;
	
	RscMgr *rscMgr;
	
	NSIndexPath *currentSelection;
	
	CableConnectState cableState;
	BOOL passRequired;
	
    BOOL loopContinous;
	BOOL loopbackTestRunning;
	int loopbackCount;
    
    BOOL rxEcho;
	
	int rxCount;
	int txCount;
    int errCount;
	
	UInt8 rxLoopBuff[kRSC_SerialReadBufferSize];
	UInt8 txLoopBuff[LOOPBACK_TEST_LEN];
    
    BOOL testingViewActive;
    
    TestViewController *testController;
    
    int currentLoopTxIndex;
}

- (NSString *)getPortConfigSettingText:(PortConfigSettingType)whichSetting;
- (void) setPortConfigSettingFromText:(NSString *)text WhichSetting:(PortConfigSettingType)whichSetting;
- (NSDictionary *)readPlist:(NSString *)plistName;
- (void) updateStats:(StatType)whichStat;
- (void)setTxMode:(BOOL)on;
- (BOOL)getTxMode;


@end
