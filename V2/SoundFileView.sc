BufSndFileView {
	
	classvar <timeMode = \frames; // or \seconds
	classvar <rateMode = \semitones; // or \ratio
	classvar <all; 
	
	var <sndFile;
	var <parent, <view, <views;
	var <>action;
	var <viewHeight = 14;
	var <>autoCreateSndFile = false;
	
	*new { |parent, bounds, action, sndFile|
		^super.new.init( parent, bounds, action ).value_( sndFile ).addToAll;
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
	
	*rateMode_ { |new = \semitones|
		rateMode = new.asSymbol;
		all.do( _.setRateMode( rateMode ) );
	}
	
	initClass {
		all = [];
	}
	
	doAction { action.value }
	
	value { ^sndFile }
	value_ { |newSndFile|
		if( sndFile != newSndFile ) {
			sndFile.removeDependant( this );
			sndFile = newSndFile;
			sndFile.addDependant( this );
			this.update;
		};
	}
	
	update {
		if( sndFile.notNil ) { this.setViews( sndFile ) };
	}
	
	resize_ { |resize|
		view.resize = resize ? 5;
	}
	
	remove {
		if( sndFile.notNil ) { 
			sndFile.removeDependant( this );
		};
		all.remove( this );
	}
	
	setViews { |inSndFile|
		{
			views[ \basename ].string = inSndFile.basename;
			views[ \dirname ].string = inSndFile.dirname;
		}.defer;
		
		views[ \startFrame ].value = inSndFile.startFrame;
		views[ \startFrame ].clipHi = inSndFile.numFrames ? inf;
		
		views[ \startSecond ].value = inSndFile.startSecond;
		views[ \startSecond ].clipHi = inSndFile.fileDuration ? inf;
		
		views[ \endFrame ].value = inSndFile.endFrame;
		views[ \endFrame ].clipHi = inSndFile.numFrames ? inf;
		
		views[ \endSecond ].value = inSndFile.endSecond;
		views[ \endSecond ].clipHi = inSndFile.fileDuration ? inf;
		
		views[ \loop ].value = inSndFile.loop.binaryValue;
		
		views[ \rateRatio ].value = inSndFile.rate;
		views[ \rateSemitones ].value = inSndFile.rate.ratiomidi.round( 1e-6);
		
	}
	
	setTimeMode { |mode = \frames|
		switch ( ( mode.asString[0] ? $f ).toLower,
			$s, { // \seconds
				views[ \startSecond ].visible_( true );
				views[ \startFrame ].visible_( false );
				views[ \endSecond ].visible_( true );
				views[ \endFrame ].visible_( false );
				{ views[ \timeMode ].value = 0 }.defer;
						
			}, 
			$f, { // \frames
				views[ \startSecond ].visible_( false );
				views[ \startFrame ].visible_( true );
				views[ \endSecond ].visible_( false );
				views[ \endFrame ].visible_( true );
				{ views[ \timeMode ].value = 1 }.defer;
			}
		);
	}
	
	setRateMode { |mode = \semitones|
		switch( ( mode.asString[0] ? $s ).toLower,
			$r, { // \ratio
				views[ \rateRatio ].visible_( true );
				views[ \rateSemitones ].visible_( false );
				{ views[ \rateMode ].value = 0 }.defer;
			}, 
			$s, { 
				views[ \rateRatio ].visible_( false );
				views[ \rateSemitones ].visible_( true );
				{ views[ \rateMode ].value = 1 }.defer;
			}
		);
	}
	
	setFont { |font|
		font = font ??
			{ RoundView.skin !? { RoundView.skin.font } } ?? 
			{ Font( Font.defaultSansFace, 10 ) };
		
		{
			views[ \basename ].font = font;
			views[ \dirname ].font = font;
			views[ \startLabel ].font = font;
			views[ \timeMode ].font = font;
			views[ \endLabel ].font = font;
			views[ \rateLabel ].font = font;
			views[ \rateMode ].font = font;
		}.defer;
		
		views[ \startFrame ].font = font;
		views[ \startSecond ].font = font;
		views[ \endFrame ].font = font;
		views[ \endSecond ].font = font;
		views[ \loop ].font = font;
		views[ \rateRatio ].font = font;
		views[ \rateSemitones ].font = font;

	}
	
	performSndFile { |selector ...args|
		if( sndFile.notNil ) {
			^sndFile.perform( selector, *args );
		} {
			if( autoCreateSndFile ) {
				this.value = BufSndFile.newBasic( );
				^sndFile.perform( selector, *args );
			} {
				^nil;
			};
		};
	}
	
	*viewNumLines { ^5 }
	
	makeView { |parent, bounds, resize|
		
		if( bounds.isNil ) { bounds= 350 @ (this.class.viewNumLines * (viewHeight + 4)) };
		
		/*
		#view, bounds = EZGui().prMakeMarginGap.prMakeView( parent, bounds );
		view.bounds = view.bounds.height_( (this.class.viewNumLines * (viewHeight + 4)) );
		view.addFlowLayout( 0@0, 4@4 );
		
		*/
		
		view = EZCompositeView( parent, bounds, gap: 4@4 );
		bounds = view.asView.bounds;
		view.onClose_({ this.remove; });
		view.resize_( resize ? 5 );
		views = ();
		
		views[ \basename ] = TextField( view, (bounds.width - (viewHeight + 4)) @ viewHeight )
			.applySkin( RoundView.skin )
			.resize_( 2 )
			.action_({ |tf|
				this.performSndFile( \basename_ , tf.string );
				action.value( this );
			});
			
		views[ \browse ] = SmoothButton( view, viewHeight @ viewHeight )
			.radius_( 0 )
			.border_(0)
			.resize_( 3 )
			.label_( 'folder' )
			.action_({
				Dialog.getPaths( { |paths|
				  this.performSndFile( \path_ , paths[0], true );
				  action.value( this );
				});
			});
			
		views[ \dirname ] = TextField( view, bounds.width @ viewHeight )
			.applySkin( RoundView.skin )
			.resize_( 2 )
			.action_({ |tf|
				this.performSndFile( \dirname_ , tf.string );
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
		
		views[ \timeMode ] = PopUpMenu( view, 40 @ viewHeight )
			.applySkin( RoundView.skin )
			.items_( [ "s", "smp" ] )
			.resize_( 3 )
			.action_({ |pu|
				this.class.timeMode = [ \seconds, \frames ][ pu.value ];
			});
			
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
		
		views[ \loop ] = SmoothButton( view, 40 @ viewHeight )
			.radius_( 3 )
			.border_( 1 )
			.resize_( 3 )
			.label_( [ "loop", "loop" ] )
			.hiliteColor_( Color.green )
			.action_({ |bt|
				this.performSndFile( \loop_ , bt.value.booleanValue );
				action.value( this );
			});
			
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
			
		this.setFont;
		this.setTimeMode( timeMode );
		this.setRateMode( rateMode );
	}
	
}

DiskSndFileView : BufSndFileView {
	
	performSndFile { |selector ...args|
		if( sndFile.notNil ) {
			^sndFile.perform( selector, *args );
		} {
			if( autoCreateSndFile ) {
				this.value = DiskSndFile.newBasic( );
				^sndFile.perform( selector, *args );
			} {
				^nil;
			};
		};
	}
}
