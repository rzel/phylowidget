package org.andrewberman.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class StringClipboard implements ClipboardOwner
	{
		public static StringClipboard instance;
		private Clipboard clip;
		private DataFlavor flavor = DataFlavor.stringFlavor;
	
		static {
			new StringClipboard();
		}
		
		private StringClipboard()
		{
			clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringClipboard.instance = this;
		}
		
		public void lostOwnership(Clipboard clipboard, Transferable contents)
		{
			// Do nothing.
		}
		
		public void toClipboard(String s)
		{
			StringSelection sel = new StringSelection(s);
			clip.setContents(sel, this);
		}
		
		public String fromClipboard()
		{
			Transferable clipboardContent = clip.getContents(this);
			if ((clipboardContent != null) &&
					clipboardContent.isDataFlavorSupported(flavor))
			{
				try
				{
					return (String)clipboardContent.getTransferData(flavor);
				} catch (UnsupportedFlavorException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				} catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
			}
			return new String("");
		}
	}