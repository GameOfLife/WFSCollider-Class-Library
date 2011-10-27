/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2011 Miguel Negrao, Wouter Snoei.

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

WFSPathBufferView {
	
	// this is not finished yet, just a copy of BufSndFileView for now
	classvar <timeMode = \seconds; // or \frames
	classvar <all; 
	
	var <wfsPathBuffer;
	var <parent, <view, <views;
	var <>action;
	var <viewHeight = 14;
	
	*new { |parent, bounds, action, wfsPathBuffer|
		^super.new.init( parent, bounds, action ).value_( wfsPathBuffer ).addToAll;
	}
	
	init { |parent, bounds, inAction|
		action = inAction;
		this.makeView( parent, bounds );
	}
	
	addToAll {
		all = all.add( this );
	}
	
	*timeMode_ { |new = \frames|
		timeMode = new.asSymbol;
		all.do( _.setTimeMode( timeMode ) );
	}

	initClass {
		all = [];
	}
	
	doAction { action.value( this ) }
	
	value { ^wfsPathBuffer }
	value_ { |newWFSPathBuffer|
		if( wfsPathBuffer != newWFSPathBuffer ) {
			newWFSPathBuffer.removeDependant( this );
			wfsPathBuffer = newWFSPathBuffer;
			newWFSPathBuffer.addDependant( this );
			this.update;
		};
	}
	
	update {
		if( wfsPathBuffer.notNil ) { this.setViews( wfsPathBuffer ) };
	}
	
	resize_ { |resize|
		view.resize = resize ? 5;
	}
	
	remove {
		if( wfsPathBuffer.notNil ) { 
			wfsPathBuffer.removeDependant( this );
		};
		all.remove( this );
	}
	
	setViews { |inWFSPathBuffer|
		// views[ \name ].value = inWFSPathBuffer.name ? "";
		views[ \filePath ].value = inWFSPathBuffer.filePath;
		views[ \loop ].value = inWFSPathBuffer.loop.binaryValue;
		if( inWFSPathBuffer.wfsPath.isWFSPath2 ) {
			views[ \miniPlot ].fromBounds = 
				inWFSPathBuffer.wfsPath.asRect.scale(1@ -1).insetBy(-2,-2);
			views[ \startSecond ].value = inWFSPathBuffer.startSecond;
		};
		views[ \startFrame ].value = inWFSPathBuffer.startFrame;
		if( inWFSPathBuffer.wfsPath.dirty ) {
			views[ \write ].background_( Color.red.alpha_(0.25) );
		} {
			views[ \write ].background_( Color.clear );
		};

	}
	
	setTimeMode { |mode = \seconds|
		switch ( ( mode.asString[0] ? $f ).toLower,
			$s, { // \seconds
				views[ \startSecond ].visible_( true );
				views[ \startFrame ].visible_( false );
				{ views[ \timeMode ].value = 0 }.defer;
						
			}, 
			$f, { // \frames
				views[ \startSecond ].visible_( false );
				views[ \startFrame ].visible_( true );
				{ views[ \timeMode ].value = 1 }.defer;
			}
		);
	}
	
	setFont { |font|
		font = font ??
			{ RoundView.skin !? { RoundView.skin.font } } ?? 
			{ Font( Font.defaultSansFace, 10 ) };
		
		{	
			views[ \fileLabel ].font = font;
			views[ \timeMode ].font = font;
		}.defer;
		
		views[ \loop ].font = font;
		views[ \plot ].font = font;
		views[ \edit ].font = font;
		views[ \write ].font = font;
		views[ \read ].font = font;
		views[ \filePath ].font = font;
		views[ \startFrame ].font = font;
		views[ \startSecond ].font = font;
		views[ \startLabel ].font = font;

	}
	
	performWFSPathBuffer { |selector ...args|
		if( wfsPathBuffer.notNil ) {
			^wfsPathBuffer.perform( selector, *args );
		} {
			^nil;
		};
	}
	
	*viewNumLines { ^5 }
	
	makeView { |parent, bounds, resize|
		
		if( bounds.isNil ) { bounds= 350 @ (this.class.viewNumLines * (viewHeight + 4)) };
		
		view = EZCompositeView( parent, bounds, gap: 4@4 );
		bounds = view.asView.bounds;
		view.onClose_({ this.remove; }).resize_( resize ? 5 );
		views = ();
		
		views[ \miniPlot ] = ScaledUserView( view, ((viewHeight * 2) + 4).asPoint )
			.fromBounds_( Rect.aboutPoint( 0@0, 100, 100 ) )
			.keepRatio_( true )
			.background_( Color.gray(0.9) )
			.drawFunc_({ |vw|
				var path;
				path = this.performWFSPathBuffer( \wfsPath );
				if( path.isWFSPath2 && { path.exists } ) {
					
					Pen.width = 0.164;
					Pen.color = Color.red(0.5, 0.5);
					
					//// draw configuration
					(WFSSpeakerConf.default ?? {
						WFSSpeakerConf.rect(48,48,5,5);
					}).draw;
					
					path.asWFSPath2.draw( 1, pixelScale: vw.pixelScale * 1.5);
				};
			});
		
		views[ \miniPlot ].view
			.canReceiveDragHandler_({ |sink|
				var drg;
				drg = View.currentDrag;
				case { drg.isKindOf( WFSPath2 ) } 
					{ true }
					{ drg.isKindOf( WFSPathURL ) }
					{ true } /*
					{ drg.isKindOf( WFSPathBuffer ) }
					{ true } */
					{ false }
			})
			.receiveDragHandler_({ |sink, x, y|
					case { View.currentDrag.isKindOf( WFSPath2 ) } {
						wfsPathBuffer.wfsPath = View.currentDrag;
						action.value( this );
					} { View.currentDrag.isKindOf( WFSPathURL ) } {
						wfsPathBuffer.wfsPath = View.currentDrag;
						action.value( this );
					};
			})
			.beginDragAction_({ 
					wfsPathBuffer.wfsPath;
			});
			
		views[ \buttonComp ] = CompositeView( view, 104@ ((viewHeight * 2) + 4) );
		views[ \buttonComp ].addFlowLayout( 0@0, 4@4 );
			
		views[ \plot ] = SmoothButton( views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 3 )
			.border_( 1 )
			.label_( "plot" )
			.action_({ |bt|
				WFSPathView( )
					.path_( wfsPathBuffer.wfsPath )
					.editMode_( \none )
					.mouseMode_( \zoom );
			});
		
		views[ \write ] = SmoothButton( views[ \buttonComp ], 60 @ viewHeight )
			.radius_( 3 )
			.border_( 1 )
			.label_( "write data" )
			.action_({ |bt|
				
				Dialog.savePanel({ |path|
				  	this.performWFSPathBuffer( \writeFile, nil, path );
				  	this.performWFSPathBuffer( \changed, \filePath );
				});
			});
			
		views[ \loop ] = SmoothButton( view, 40 @ viewHeight )
			.radius_( 3 )
			.border_( 1 )
			.label_( [ "loop", "loop" ] )
			.hiliteColor_( Color.green )
			.action_({ |bt|
				this.performWFSPathBuffer( \loop_ , bt.value.booleanValue );
				action.value( this );
			});
			
		views[ \buttonComp ].decorator.nextLine;

		views[ \edit ] = SmoothButton( views[ \buttonComp ], 40 @ viewHeight )
			.radius_( 3 )
			.border_( 1 )
			.label_( "edit" )
			.action_({ |bt|
				WFSPathGUI( object: wfsPathBuffer.wfsPath )
					.action_({ |editor|
						wfsPathBuffer.wfsPath = editor.object;
						views[ \miniPlot ].refresh;
						action.value( this );
					}); 
			});	
		
					
		views[ \read ] = SmoothButton( views[ \buttonComp ], 60 @ viewHeight )
			.radius_( 3 )
			.border_( 1 )
			.label_( "read data" )
			.action_({ |bt|
				Dialog.getPaths({ |paths|
					var sf, pth, wfspath, fa;
					pth = paths[0];
					sf = SoundFile.openRead( pth );
					if( sf.notNil ) {
						if( sf.numChannels == 9 ) {
							fa = FloatArray.newClear( sf.numFrames * sf.numChannels );
							sf.readData( fa );
							wfspath = WFSPath2.fromBufferArray( fa );
							wfspath.name = pth.basename.removeExtension;
							wfspath.filePath = pth;
							wfspath.savedCopy = wfspath.deepCopy;
							this.performWFSPathBuffer( \filePath_ , pth );
							this.performWFSPathBuffer( \wfsPath_ , wfspath );
							if( wfsPathBuffer.notNil ) {
								this.setViews( wfsPathBuffer );
							};
							action.value( this );
						} {
							"wrong number of channels (% instead of 9)\n"
								.postf( sf.numChannels );
						};
						sf.close;
					} {
						"could not read file '%'\n".postf( pth );
					};
				});
			});
		
			
		view.view.decorator.nextLine;
		
		views[ \fileLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.string_( "file" );
		
		views[ \filePath ] = FilePathView( view, 
			(bounds.width - (30 + 4)) @ ( (viewHeight * 2) + 4 ) )
			.resize_( 2 )
			.action_({ |fv|
				this.performWFSPathBuffer( \filePath_ , fv.value );
				action.value( this );
			});	

			
		views[ \startLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.string_( "start" );
		
		views[ \startComp ] = CompositeView( view, (bounds.width - 78) @ viewHeight )
			.resize_( 2 );
		
		views[ \startSecond ] = SMPTEBox( views[ \startComp ], 
				views[ \startComp ].bounds.moveTo(0,0) )
			.applySmoothSkin
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performWFSPathBuffer( \startSecond_ , nb.value );
				action.value( this );
			});
			
		views[ \startFrame] = SmoothNumberBox( views[ \startComp ], 
				views[ \startComp ].bounds.moveTo(0,0) )
			.resize_( 5 )
			.clipLo_( 0 )
			.scroll_step_( 0.1 )
			.action_({ |nb|
				this.performWFSPathBuffer( \startFrame_ , nb.value );
				action.value( this );
			})
			.visible_( false );
			
		views[ \timeMode ] = PopUpMenu( view, 40 @ viewHeight )
			.applySkin( RoundView.skin )
			.items_( [ "s", "fr" ] )
			.resize_( 3 )
			.action_({ |pu|
				this.class.timeMode = [ \seconds, \frames ][ pu.value ];
			});

		
		/*	
		views[ \startLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.string_( "start" );
		
		views[ \startComp ] = CompositeView( view, (bounds.width - 78) @ viewHeight )
			.resize_( 2 );
		
		views[ \startSecond ] = SMPTEBox( views[ \startComp ], 
				views[ \startComp ].bounds.moveTo(0,0) )
			.applySmoothSkin
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performSndFile( \startSecond_ , nb.value );
				action.value( this );
			});
			
		views[ \startFrame] = SmoothNumberBox( views[ \startComp ], 
				views[ \startComp ].bounds.moveTo(0,0) )
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performSndFile( \startFrame_ , nb.value );
				action.value( this );
			})
			.visible_( false );
			
		views[ \endLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.string_( "end" );
		
		views[ \endComp ] = CompositeView( view, (bounds.width - 78) @ viewHeight )
			.resize_( 2 );
		
		views[ \endSecond ] = SMPTEBox( views[ \endComp ], 
				views[ \endComp ].bounds.moveTo(0,0) )
			.applySmoothSkin
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performSndFile( \endSecond_ , nb.value );
				action.value( this );
			});
			
		views[ \endFrame] = SmoothNumberBox( views[ \endComp ], 
				views[ \endComp ].bounds.moveTo(0,0) )
			.resize_( 5 )
			.clipLo_( 0 )
			.action_({ |nb|
				this.performSndFile( \endFrame_ , nb.value );
				action.value( this );
			})
			.visible_( false );
			
		views[ \rateLabel ] = StaticText( view, 30 @ viewHeight )
			.applySkin( RoundView.skin )
			.string_( "rate" );
			
		views[ \rateComp ] = CompositeView( view, (bounds.width - 118) @ viewHeight )
			.resize_( 2 );
		
		views[ \rateRatio ] = SmoothNumberBox( views[ \rateComp ], 
				views[ \rateComp ].bounds.moveTo(0,0) )
			.resize_( 5 )
			.scroll_step_( 0.1 )
			.clipLo_( 0 )
			.value_( 1 )
			.action_({ |nb|
				this.performSndFile( \rate_ , nb.value );
				action.value( this );
			});
			
		views[ \rateSemitones ] = SmoothNumberBox( views[ \rateComp ], 
				views[ \rateComp ].bounds.moveTo(0,0) )
			.resize_( 5 )
			.action_({ |nb|
				this.performSndFile( \rate_ , nb.value.midiratio );
				action.value( this );
			})
			.visible_( false );
			
		views[ \rateMode ] = PopUpMenu( view, 80 @ viewHeight )
			.applySkin( RoundView.skin )
			.items_( [ "ratio", "semitones" ] )
			.resize_( 3 )
			.action_({ |pu|
				this.class.rateMode = [ \ratio, \semitones ][ pu.value ];
			});
			
		
		this.setTimeMode( timeMode );
		this.setRateMode( rateMode );
		*/
		
		this.setFont;
	}
	
}

