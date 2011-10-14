/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

WFSPlotSmooth {
	classvar <>window, <>view;
	classvar <>playButton;
	
	*new { |title = "WFS", bounds, toFront = true, removeButtons = true|
		bounds = bounds ? Rect(128, 64, 400, 400);
		if( this.isOpen )
			{ 	window.onClose.value;
				if( removeButtons )
					{ if(playButton.notNil) { playButton.remove; playButton = nil; };  
					/*
					 window.view.children.do({ |view|
						 if( view.class == RoundButton )
						 	{ view.remove }; });
					*/
				};

					
				window.name = title.asString ++ ":plotSmooth";
				//this.addDragView;
				if( toFront ) { window.front };
			}
			{ window = SCWindow( title.asString ++ ":plotSmooth", bounds ).front;
			  window.view.background_( Color.black );
			  this.addDragView( window );
			  playButton = nil;
			};
		
		^window;
		}
	
	window { ^window }
	
	*addDragView { |theWindow| 
		view = UserView( theWindow, theWindow.view.bounds )
			.resize_(5)
			.canReceiveDragHandler_( { SCView.currentDrag.respondsTo( \plotSmooth ) })
			.receiveDragHandler_( { SCView.currentDrag.plotSmooth; } )
			.background_(Color.black)
			

		
		   }
	
	*isOpen { ^( window.notNil && { window.dataptr.notNil } ); }
	*isClosed { ^( window.notNil && { window.dataptr.notNil } ).not; }
	
	*keep { window = nil; } // next *new call will open new window
	
	*close { if( this.isOpen ) { window.close } }
	
	}
	
	
	