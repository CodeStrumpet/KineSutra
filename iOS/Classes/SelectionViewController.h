//
//  SelectionViewController.h
//  Rsc Demo
//
//  Copyright © 2011 Redpark  All Rights Reserved
//

#import <UIKit/UIKit.h>


@protocol SelectionViewControllerDelegate;


@interface SelectionViewController : UITableViewController {

	NSArray *tableData;
	int selected;
	UIColor *oddCellColor;
	UIColor *evenCellColor;
	
	id <SelectionViewControllerDelegate> theDelegate;
}


@property (readwrite, retain) NSArray *tableData;
@property (readwrite, assign) int selected;
@property (readwrite, retain) UIColor *oddCellColor;
@property (readwrite, retain) UIColor *evenCellColor;

- (void) setDelegate:(id)delegate;

@end

@protocol SelectionViewControllerDelegate

- (void)selectionController:(SelectionViewController *)selectionController didSelectIndex:(int)index;


@end