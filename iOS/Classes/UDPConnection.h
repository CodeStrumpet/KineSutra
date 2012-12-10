//
//  UDPConnection.h
//  DinoLasers
//
//  Created by Paul Mans on 11/27/12.
//  Copyright (c) 2012 DinoLasers. All rights reserved.
//

#import <CoreMotion/CoreMotion.h>
#import <Foundation/Foundation.h>
#import "GCDAsyncUdpSocket.h"

#define DEFAULT_HOST_PORT 10552
#define DEFAULT_LOCAL_PORT 0
#define DEFAULT_HOST @"localhost"

@class UDPConnection;

@protocol UDPConnectionDelegate <NSObject>

- (void)UDPConnection:(UDPConnection *)theUDPConnection didReceiveMessage:(NSString *)message fromHost:(NSString *)theHost onPort:(int)thePort;

@end

@interface UDPConnection : NSObject

@property (nonatomic, strong) GCDAsyncUdpSocket *udpSocket;
@property (nonatomic, strong) NSString *socketHost;
@property (nonatomic, assign) int hostPort;
@property (nonatomic, assign) int localPort;
@property (nonatomic, assign) id <UDPConnectionDelegate> delegate;

/**
 *  Will instantiate the socket with the current host and port values
 */
- (void)setupSocket;

- (void)close;

- (void)sendData:(NSData *)data toHost:(NSString *)host port:(int)port withTimeout:(int)timeout tag:(long)tag;

- (void)sendMessage:(NSString *)message withTag:(long)tag;

@end
