package org.vivecraft.utils;

public class TransactionMutex {
	public boolean approved;
	public Thread watchdog;
	public boolean consumed;
}
