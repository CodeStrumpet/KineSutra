//
//  RootViewController.m
//  Rsc Demo
//
//  Copyright © 2011-2012 Redpark  All Rights Reserved
//

#import "RootViewController.h"
#import "SelectionViewController.h"
#import "TestViewController.h"
#import "RscMgr.h"

enum 
{
	kSectionCableStatus = 0,
	kNumCableStatus = 1,
	kSectionPortConfig = 1,
	kSectionStats = 2,
	kNumStats = 3,
	
};


#define MODEM_STAT_ON_COLOR [UIColor colorWithRed:0.0/255.0 green:255.0/255.0 blue:0.0/255.0 alpha:1.0]
#define MODEM_STAT_OFF_COLOR [UIColor colorWithRed:157.0/255.0 green:157.0/255.0 blue:157.0/255.0 alpha:1.0]
#define MODEM_STAT_RECT CGRectMake(0.0f,0.0f,42.0f,21.0f)

#define TABLE_DATA_PLIST @"PortconfigStrings"

#define CABLE_CONNECTED_TEXT @"Connected";
#define CABLE_NOT_CONNECTED_TEXT @"Not Connected";
#define CABLE_REQUIRES_PASSCODE_TEXT @"Passcode Required"



@implementation RootViewController


#pragma mark -
#pragma mark View lifecycle




- (void)viewDidLoad {
    [super viewDidLoad];
    
    // Create the Test View - this is where rx bytes are dumped to a scrollable text area
    // user can type bytes to send or initiate a loopback test
    testingViewActive = NO;
    testController = [[TestViewController alloc] initWithNibName:@"TestViewController" bundle:nil];

    // Add butons to bottom toolbar for turning on/ff RTS and DTR.  This demonstrates how
    // to enable/disable the modem leads
	rtsButton = [[[UIBarButtonItem alloc] initWithTitle:@"RTS" style: UIBarButtonItemStyleBordered target:self action:@selector(toggleRTS)] autorelease];
	[rtsButton setEnabled:FALSE];

	dtrButton = [[[UIBarButtonItem alloc] initWithTitle:@"DTR" style: UIBarButtonItemStyleBordered target:self action:@selector(toggleDTR)] autorelease];
	[dtrButton setEnabled:FALSE];

	
	cableState = kCableNotConnected; // maintain our own state variable for cable status
	passRequired = NO; // ignore for future use
	
    //////////////////////////////////////////////////////////////////////////
    // Create and initialize our RedparkSerialCable Manager for communicating
    // with the serial cable.
    rscMgr = [[RscMgr alloc] init]; 
    
    // Set this as our delegate so we can receive evevnts
	[rscMgr setDelegate:self];
	
    // For convenience the strings which populate the various port config screens
    // are built up from a plist.
	portConfigTableData = [self readPlist:TABLE_DATA_PLIST];
	portConfigKeys = [[portConfigTableData allKeys]
					  sortedArrayUsingSelector:@selector(localizedCaseInsensitiveCompare:)];
	[portConfigKeys retain];
	
    // Create modem status indicators on the toolbar
	NSMutableArray *modemStatusButtons = [[NSMutableArray alloc] init];	
	UIBarButtonItem *barButton;
	
	ctsLabel = [[UILabel alloc] initWithFrame:MODEM_STAT_RECT];
	[ctsLabel setBackgroundColor:[UIColor clearColor]];
	[ctsLabel setTextColor:MODEM_STAT_OFF_COLOR];
	[ctsLabel setTextAlignment:UITextAlignmentCenter];
	ctsLabel.text = @"CTS";
	ctsLabel.font = [UIFont boldSystemFontOfSize:17.0];
	barButton = [[[UIBarButtonItem alloc] initWithCustomView:ctsLabel] autorelease];
	[modemStatusButtons addObject:barButton];
		
	dsrLabel = [[UILabel alloc] initWithFrame:MODEM_STAT_RECT];
	[dsrLabel setBackgroundColor:[UIColor clearColor]];
	[dsrLabel setTextColor:MODEM_STAT_OFF_COLOR];
	[dsrLabel setTextAlignment:UITextAlignmentCenter];
	dsrLabel.text = @"DSR";
	dsrLabel.font = [UIFont boldSystemFontOfSize:17.0];

	barButton = [[[UIBarButtonItem alloc] initWithCustomView:dsrLabel] autorelease];
	[modemStatusButtons addObject:barButton];

	cdLabel = [[UILabel alloc] initWithFrame:MODEM_STAT_RECT];
	[cdLabel setBackgroundColor:[UIColor clearColor]];
	[cdLabel setTextColor:MODEM_STAT_OFF_COLOR];
	[cdLabel setTextAlignment:UITextAlignmentCenter];
	cdLabel.text = @"CD";
	cdLabel.font = [UIFont boldSystemFontOfSize:17.0];

	barButton = [[[UIBarButtonItem alloc] initWithCustomView:cdLabel] autorelease];
	[modemStatusButtons addObject:barButton];

	riLabel = [[UILabel alloc] initWithFrame:MODEM_STAT_RECT];
	[riLabel setBackgroundColor:[UIColor clearColor]];
	[riLabel setTextColor:MODEM_STAT_OFF_COLOR];
	[riLabel setTextAlignment:UITextAlignmentCenter];
	riLabel.text = @"RI";
	riLabel.font = [UIFont boldSystemFontOfSize:17.0];

	barButton = [[[UIBarButtonItem alloc] initWithCustomView:riLabel] autorelease];
	[modemStatusButtons addObject:barButton];
	
	[modemStatusButtons addObject:rtsButton];
	[modemStatusButtons addObject:dtrButton];
	
	
	[self setToolbarItems:modemStatusButtons animated:NO];
	
	[modemStatusButtons release];
	
	self.navigationItem.rightBarButtonItem.target = self;

    self.navigationItem.rightBarButtonItem.action = @selector(animateToTestView);

    loopContinous = NO;
	loopbackTestRunning = NO;
    rxEcho = NO;
	rxCount = 0;
	txCount = 0;
    errCount = 0;
    
    

	
}

// reads a plist file from the bundle directory and
// and returns a dictionary with the data
//
// plistName is name of file minus the suffix (i.e. .plist)
- (NSDictionary *)readPlist:(NSString *)plistName
{
	NSBundle *thisBundle = [NSBundle bundleForClass:[self class]];
	NSString *commonDictionaryPath;
	NSDictionary *theDictionary = nil;
	
	if ((commonDictionaryPath = [thisBundle pathForResource:plistName ofType:@"plist"]))  {
		theDictionary = [[NSDictionary alloc] initWithContentsOfFile:commonDictionaryPath];
		
		// when completed, it is the caller's responsibility to release theDictionary
	}
	
	if (!theDictionary)
	{
		NSLog(@"readPlist - Unable to load %@", plistName);
	}
	
	return theDictionary;
}



#pragma mark -
#pragma mark Memory management

- (void)didReceiveMemoryWarning {
    // Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
    
    // Relinquish ownership any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
    // Relinquish ownership of anything that can be recreated in viewDidLoad or on demand.
    // For example: self.myOutlet = nil;
    [testController release];
}


- (void)dealloc {
	
	if (riLabel) [riLabel release];
	if (dsrLabel) [dsrLabel release];
	if (ctsLabel) [ctsLabel release];
	if (cdLabel) [cdLabel release];
	
	if (portConfigKeys) [portConfigKeys release];
	if (portConfigTableData) [portConfigTableData release];
	
	if (rscMgr) [rscMgr release];
    [super dealloc];
}


#pragma mark -
#pragma mark Table view data source

// Customize the number of sections in the table view.
- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 4;
}


// Customize the number of rows in the table view.
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    
	int nRows = 0;
	
	switch(section)
	{
		case kSectionCableStatus:
			nRows = kNumCableStatus;
			break;
		case kSectionPortConfig:
			nRows = [[portConfigTableData allKeys] count];
			break;
		case kSectionStats:
			nRows = kNumStats;
			break;
		default:
			nRows = 0;
			break;
	}
	
	return nRows;
}


// Customize the appearance of table view cells.
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    
    static NSString *CellIdentifier = @"RscDemoCell";
	NSString *detailText = nil;
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:CellIdentifier] autorelease];
    }
	
	
    
	if (indexPath.row % 2)
	{
        [cell setBackgroundColor:[UIColor colorWithRed:.51 green:.51 blue:.51 alpha:1]];
	}
	else [cell setBackgroundColor:[UIColor colorWithRed:.61 green:.61 blue:.61 alpha:1]];
	
	// Configure the cell.
	
	switch(indexPath.section)
	{
		case kSectionCableStatus:
			cell.textLabel.text = @"Cable Status";
			cell.detailTextLabel.text = (cableState == kCableConnected) ? @"Connected" : @"Not Connected";
			break;
		case kSectionPortConfig:
			
			cell.textLabel.text = [portConfigKeys objectAtIndex:indexPath.row]; 
			detailText = [self getPortConfigSettingText:indexPath.row];
			cell.detailTextLabel.text = detailText;
			[detailText release];
			
			cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
			
			break;
		case kSectionStats:
			switch (indexPath.row) 
			{
                case 0:
                    cell.textLabel.text = @"Rx";
                    cell.detailTextLabel.text = [NSString stringWithFormat:@"%d", rxCount];
                    break;
                case 1:
                    cell.textLabel.text = @"Tx";
                    cell.detailTextLabel.text = [NSString stringWithFormat:@"%d", txCount];
                    break;
                case 2:
                    cell.textLabel.text = @"Errors";
                    cell.detailTextLabel.text = [NSString stringWithFormat:@"%d", errCount];
                    break;
            }
			break;
		
	}
	
	//
    return cell;
}

#pragma mark -
#pragma mark Table view delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    
	UITableViewCell *cell = [tableView cellForRowAtIndexPath:indexPath];
	
	if (currentSelection) [currentSelection release];
	currentSelection = [[NSIndexPath indexPathForRow:indexPath.row inSection:indexPath.section]retain];
	
	switch (indexPath.section)
	{
		case kSectionPortConfig:
		{
			SelectionViewController *detailViewController = [[SelectionViewController alloc] initWithNibName:@"SelectionViewController" bundle:nil];
			detailViewController.tableData =  [portConfigTableData objectForKey:[portConfigKeys objectAtIndex:indexPath.row]];
			detailViewController.selected = [detailViewController.tableData indexOfObject:cell.detailTextLabel.text];
			detailViewController.evenCellColor = [UIColor colorWithRed:.51 green:.51 blue:.51 alpha:1];
			detailViewController.oddCellColor = [UIColor colorWithRed:.61 green:.61 blue:.61 alpha:1];
			[detailViewController setDelegate:self];
			// ...
			// Pass the selected object to the new view controller.
			[self.navigationController pushViewController:detailViewController animated:YES];
			[detailViewController release];
			break;
		}
		default:
			[tableView deselectRowAtIndexPath:indexPath animated:YES];
			break;
	}
	 
}

#pragma mark -
#pragma mark SelectionViewController delegate

- (void)selectionController:(SelectionViewController *)selectionController didSelectIndex:(int)index
{
	NSString *temp = [selectionController.tableData objectAtIndex:selectionController.selected];
	
	
	UITableViewCell *currentCell = [self.tableView cellForRowAtIndexPath:currentSelection];
	currentCell.detailTextLabel.text = temp;
	
	// set port config with new value
	[self setPortConfigSettingFromText:temp WhichSetting:(PortConfigSettingType)currentSelection.row];
	
	
}

#pragma mark -
#pragma mark RSC interface 

- (NSString *)getPortConfigSettingText:(PortConfigSettingType)whichSetting
{
	NSString *temp = nil;
	serialPortConfig portCfg;
	[rscMgr getPortConfig:&portCfg];
	
	NSArray *portConfigValues = [portConfigTableData objectForKey:[portConfigKeys objectAtIndex:whichSetting]];
	int value = 0;
	switch(whichSetting)
	{
		case kBaudIndex:
			// baud
			value = [rscMgr getBaud];
			temp = [[NSString alloc]initWithFormat:@"%d",value];
			break;
		case kDataBitsIndex:
			value = portCfg.dataLen;
			temp = [[NSString alloc] initWithFormat:@"%d",value];
			break;
		case kParityIndex:
			temp = [[NSString alloc] initWithString:[portConfigValues objectAtIndex:portCfg.parity ]];
			break;
		case kStopBitsIndex:
			value = portCfg.stopBits;
			temp = [[NSString alloc] initWithFormat:@"%d",value];
			break;
			
	}
	
	
	return temp;
	
}

- (void) setPortConfigSettingFromText:(NSString *)text WhichSetting:(PortConfigSettingType)whichSetting
{
	int value = [text intValue];
	switch(whichSetting)
	{
		case kBaudIndex:
			// baud
            
             if ([rscMgr supportsExtendedBaudRates] ==  NO && value > 57600)
             {
                 UIAlertView *alert = [[UIAlertView alloc]initWithTitle:@"Invalid Baud Rate" message:@"This cable does not support extended baud rates > 57600." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
                 
                 [alert show];
                 
                 [alert release];
             }
            
			[rscMgr setBaud:value];
			break;
		case kDataBitsIndex:
			[rscMgr setDataSize:(DataSizeType)value];
			break;
		case kParityIndex:
        {
            // Match parity string to string table and use index to set parity value
            NSArray *portConfigValues = [portConfigTableData objectForKey:[portConfigKeys objectAtIndex:kParityIndex]];

            value = [portConfigValues indexOfObject:text];
			[rscMgr	setParity:(ParityType)value];
			break;
        }
		case kStopBitsIndex:
			[rscMgr setStopBits:(StopBitsType)value];
			break;
			
	}
}


- (void) toggleRTS
{
	BOOL rtsState = [rscMgr getRts];
	
	rtsState = !rtsState;
	
	if (rtsState) rtsButton.style = UIBarButtonItemStyleDone;
	else rtsButton.style = UIBarButtonItemStyleBordered;
	
	[rscMgr setRts:rtsState];
}

- (void) toggleDTR
{
	BOOL dtrState = [rscMgr getDtr];
	
	dtrState = !dtrState;
	
	if (dtrState) dtrButton.style = UIBarButtonItemStyleDone;
	else dtrButton.style = UIBarButtonItemStyleBordered;
	
	[rscMgr setDtr:dtrState];
}

-(void)animateToTestView
{
	testController.delegate = self;
    testingViewActive = YES;
	testController.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;
    [self.navigationController pushViewController:testController animated:YES];
    self.navigationController.title = @"Testing...";
    
	//[testController release];
}

- (void) doLoopbackTest
{
    /*
     This demonstrates how to read/write data to the serial port using the rsc mgr.
     
     The loop test has been in enhanced in this version (1.1.7) to show how to use the
     txAck feature for sending large data messages to devices that don't use
     hardware/software flow control.  Otherwise, its possible to overrun the cable's
     internal tx fifo for the serial port.  See setTxMode(),getTxMode() and the portStatusChanged()
     functions.
     
     */
    
    
    //Set the tx mode to O
    [self setTxMode:YES]; 
    
    int i;
	char c;
    
	for (i = 0; i < LOOPBACK_TEST_LEN; i++)
	{
		if (!(i % 26)) c = 'A';
		txLoopBuff[i] = c++;
	}
	
	loopbackCount = 0;
	loopbackTestRunning = YES;	
	txCount += kRsc_TxFifoSize;
    currentLoopTxIndex = 0;
	[rscMgr write:txLoopBuff length:kRsc_TxFifoSize];
	[self updateStats:kStatTx];
     
    
}

- (BOOL)getTxMode
{
    serialPortConfig portCfg;
	[rscMgr getPortConfig:&portCfg];
    
    if(portCfg.txAckSetting == 1) return YES;
    else return NO;
    
}

- (void)setTxMode:(BOOL)on
{
    serialPortConfig portCfg;
	[rscMgr getPortConfig:&portCfg];
    
    if(on == YES) 
    {
        if(portCfg.txAckSetting == 1) return;
        portCfg.txAckSetting = 1;
    }
    else 
    {
        if(portCfg.txAckSetting == 0) return;
        portCfg.txAckSetting = 0;
    }
    
    [rscMgr setPortConfig:&portCfg requestStatus: NO];
    
}

- (void) updateStats:(StatType)whichStat 
{
	NSIndexPath *path = [NSIndexPath indexPathForRow:whichStat inSection:kSectionStats];
	UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:path];
	
    int value = 0;
    switch(whichStat)
    {
        case kStatRx:
            value = rxCount;
            break;
        case kStatTx:
            value = txCount;
            break;
        case kStatErr:
            value = errCount;
            break;
    }
    
	cell.detailTextLabel.text = [NSString stringWithFormat:@"%d", value];
}

#pragma mark -
#pragma mark RSC delegate


- (void) cableConnected:(NSString *)protocol
{
	// get cell for device status
	NSIndexPath *path = [NSIndexPath indexPathForRow:0 inSection:kSectionCableStatus];
	UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:path];
	
	cell.detailTextLabel.text = CABLE_CONNECTED_TEXT;
	
	cableState = kCableConnected;
	
	// We display the serial config options
	// to the user so we assume they've been set already.
	
	
	// Now open the serial communication session using the
	// serial port configuration options we've already set.
	// However, the baud rate, data size, parity, etc....
	// can be changed after calling open if needed.
	[rscMgr open];
    
    // In general, this would be a good place to setBaud, setDataSize, etc...
	// Example
	// [rscMgr setBaud:57600];
		
	// Cable connected so enable dtr and rts toggle buttons
	if (rtsButton != nil)
	{
		[rtsButton setEnabled:TRUE];
		[dtrButton setEnabled:TRUE];
	}
    
    
}


- (void) cableDisconnected
{
	// get cell for device status
	NSIndexPath *path = [NSIndexPath indexPathForRow:0 inSection:kSectionCableStatus];
	UITableViewCell *cell = [self.tableView cellForRowAtIndexPath:path];
	
	cell.detailTextLabel.text = CABLE_NOT_CONNECTED_TEXT;
	
	cableState = kCableNotConnected;
	passRequired = NO;
	
	if (rtsButton != nil)
	{
		[rtsButton setEnabled:FALSE];
		[dtrButton setEnabled:FALSE];
	}
	
	loopbackTestRunning = NO;
    loopContinous = NO;
    rxEcho = NO;
	rxCount = 0;
	txCount = 0;
    errCount = 0;
	[self updateStats:kStatRx];
	[self updateStats:kStatTx];
    
    [testController reset];
	
}


- (void) portStatusChanged
{
	int modemStatus = [rscMgr getModemStatus];
	static serialPortStatus portStat;
    
	[ctsLabel setTextColor:(modemStatus & MODEM_STAT_CTS) ? MODEM_STAT_ON_COLOR : MODEM_STAT_OFF_COLOR];
	[riLabel setTextColor:(modemStatus & MODEM_STAT_RI) ? MODEM_STAT_ON_COLOR : MODEM_STAT_OFF_COLOR];
	[dsrLabel setTextColor:(modemStatus & MODEM_STAT_DSR) ? MODEM_STAT_ON_COLOR : MODEM_STAT_OFF_COLOR];
	[cdLabel setTextColor:(modemStatus & MODEM_STAT_DCD) ? MODEM_STAT_ON_COLOR : MODEM_STAT_OFF_COLOR];
    
    [rscMgr getPortStatus:&portStat];
    
    if(loopbackTestRunning == YES && portStat.txAck)
    {
        // We received txAck so we know tx fifo in cable is empty.
        // Send the next chunk
        
        currentLoopTxIndex += kRsc_TxFifoSize;
        
        // For continous mode allow currentLoopTxIndex to wrap so it keeps going
        // otherwise we'll stop sending when the full TEST_LEN amount has been sent
        if (loopContinous == YES)
        {
            currentLoopTxIndex = (currentLoopTxIndex % kRsc_TxFifoSize);
        }
        
        // Note - this code assumes LOOPBACK_TEST_LEN
        // is an even multiple of the kRsc_TxFifoSize.  This
        // is intentionally made simple.  However, in a real application, 
        // you would want to check the actual remaining len and only send that
        // on the last chunk instead of the full kRsc_TxFifoSize.  You can also
        // send smaller chunk sizes this example just demonstrates how to send the max
        // chunk size without overruning the tx fifo.
        if(currentLoopTxIndex < LOOPBACK_TEST_LEN)
        {
            txCount += [rscMgr write:txLoopBuff+currentLoopTxIndex length:kRsc_TxFifoSize];
            [self updateStats:kStatTx];
        }
        
        
    }

}

- (void) readBytesAvailable:(UInt32)numBytes
{
	int bytesRead; 
    BOOL res = NO;
	
	// Read the data out
	bytesRead = [rscMgr read:(rxLoopBuff+loopbackCount) length:numBytes];
	rxCount += bytesRead;
	
    //NSLog(@"Read %i, total=%i\n", bytesRead, rxCount);
    
    
    
    // update stats display
	[self updateStats:kStatRx];
    
    if (loopContinous == YES)
    {
        
        return;
    }

    
    // Check our various test options bits
    if (rxEcho == YES)
    {
        [rscMgr write:rxLoopBuff length:bytesRead];
        return;
    }
    
    
	if (loopbackTestRunning == YES)
	{
		loopbackCount += bytesRead;
		if (loopbackCount >= LOOPBACK_TEST_LEN)
		{
			
			if (memcmp(rxLoopBuff, txLoopBuff, LOOPBACK_TEST_LEN) == 0)
			{
                res = YES;
			}
			else
			{
                errCount++;
                [self updateStats:kStatErr];
                res = NO;
			}
            

            // if not in continous mode (ie simple loop test)
            // report the results and end test
            loopbackTestRunning = NO;
            NSString *resultStr = [NSString stringWithString:(res == YES) ? @"Success" : @"Failed"];
            
            UIAlertView *alert = [[UIAlertView alloc]initWithTitle:@"Loop Test" message:resultStr delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
            
            [alert show];
            
            [alert release];
           
            
            loopbackCount = 0;
            
		}
        
	}
    
    // Update display if test controller is visible
    if(testingViewActive == YES)
    {
        
        NSString *s = [[NSString alloc] initWithBytes:rxLoopBuff length:bytesRead encoding: NSUTF8StringEncoding];
        
        NSMutableString *s1 = [[NSMutableString alloc] initWithString:testController.rxText.text];
        
        if (s != nil && s1 != nil) 
        {
            
            [s1 appendString:s];
            
            int len = [s1 length];
            
            //limit to 3000 bytes in the Rx window
            
            if(len > 3000) 
            {
                NSString *s2 = (NSMutableString *)[s1 substringFromIndex:(len - 3000)];  
                len = [s1 length];
                //NSLog(@"new len=%i\n", len);
                testController.rxText.text= s2;
            }
            else
            {
                testController.rxText.text= s1;
            }
            
            testController.rxText.selectedRange = NSMakeRange([testController.rxText.text length], 0);
            
            
            [s release];
            [s1 release];
        }
    }

}

//The method that will be called back:
- (void)startLoopbackTestCallBack
{
	[self doLoopbackTest];
}

- (void)startStopTxTest:(BOOL)start
{
    if (loopContinous == YES)
    {
        // already running so return
        if (start == YES) return;
        
        // its running so stop the test
        loopContinous = NO;
        loopbackTestRunning = NO;
        return;
    }
    
    // the test isn't running so just return
    if (start == NO) return;
    
    loopContinous = YES;
    [self doLoopbackTest];
}

- (void)startStopRxTest:(BOOL)start
{
    loopbackTestRunning = NO;
    loopContinous = NO;
    
    rxEcho = YES;
}

//The method that will be called back:
- (void)sendTextCallBack:(NSString *)s
{
    NSData *someData = [s dataUsingEncoding:NSUTF8StringEncoding];
    const void *bytes = [someData bytes];
    int length = [someData length];
    
    loopbackCount = 0;
    [rscMgr write:(UInt8 *)bytes length:length];
    txCount += length; // JMB make sure stats display is accurrate using new test window
	[self updateStats:kStatTx];
}

//The method that will be called back:
- (void)testingViewFinished
{
	testingViewActive = NO;
}

@end
