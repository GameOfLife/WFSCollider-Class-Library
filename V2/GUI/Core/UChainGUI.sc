UChainGUI {
	
	classvar <>skin;
	
	var <chain;
	
	var <parent, <composite, <views, <startButton, <uguis, <controller;
	var <>action;
	var originalBounds;
	
	*initClass {
		StartUp.defer({
			skin = ( 
				labelWidth: 80, 
				font: Font( Font.defaultSansFace, 10 ), 
				hiliteColor: Color.gray(0.33)
			);
		});
	}
	
	*new { |parent, bounds, chain|
		^super.newCopyArgs( chain ).init( parent, bounds );
	}
	
	init { |inParent, bounds|
		parent = inParent;
		
		if( parent.isNil ) { 
			parent = chain.class.asString;
		};
		if( parent.class == String ) {
			
			parent = Window(
				parent, 
				bounds ?? { Rect(128 rrand: 256, 64 rrand: 128, 342, 400) }, 
				scroll: true
			).front;
		};
		
		this.makeViews( bounds );
	}
	
	makeViews { |bounds|
		RoundView.useWithSkin( skin ++ (RoundView.skin ? ()), {
			this.prMakeViews( bounds );
		});
	}
	
	getHeight { |units, margin, gap|
		^units.collect({ |unit|
			UGUI.getHeight( unit, 14, margin, gap ) + 14 + gap.y + gap.y;
		}).sum + (4 * (14 + gap.y));
	}
	
	prMakeViews { |bounds|
		var margin = 0@0, gap = 4@4;
		var heights, units;
		var labelWidth, releaseTask;
		// var unitInitFunc;
		
		labelWidth = 80;
		
		if( RoundView.skin.notNil ) { labelWidth = RoundView.skin.labelWidth ? 80 };
		
		views = ();
		
		originalBounds = bounds.copy;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		if( parent.asView.class.name == 'SCScrollTopView' ) {
			bounds.width = bounds.width - 12;
		};
		units = chain.units.collect({ |u| 
			if( u.class == MetaU ) { u.unit; } { u; }
		});
		
		this.getHeight( units, margin, gap );
				
		controller = SimpleController( chain );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove; };
		
		// startbutton
		views[ \startButton ] = SmoothButton( composite, 14@14 )
			.label_( ['power', 'power'] )
			.hiliteColor_( Color.green.alpha_(0.5) )
			.action_( [ { 
					var startAction;
					releaseTask.stop;
					if( chain.releaseSelf or: (chain.dur == inf) ) {
						chain.prepareAndStart;
					} {
						startAction = { 
							chain.start;
							releaseTask = {
								(chain.dur - chain.fadeOut).wait;
								chain.release;
							}.fork;
						};
						chain.prepare( action: startAction );
							
					};
				}, { 
					releaseTask.stop;
					chain.release 
				} ]
		 	);
			
		if( chain.groups.size > 0 ) {
			views[ \startButton ].value = 1;
		};
			
		composite.decorator.nextLine;
		
		// startTime
		StaticText( composite, labelWidth@14 )
			.applySkin( RoundView.skin )
			.string_( "startTime" )
			.align_( \right );
			
		views[ \startTime ] = SMPTEBox( composite, 84@14 )
			.applySmoothSkin
			.applySkin( RoundView.skin )
			.clipLo_(0)
			.action_({ |nb|
				chain.startTime_( nb.value );
			});
		
		composite.decorator.nextLine;
		
		// duration
		StaticText( composite, labelWidth@14 )
			.applySkin( RoundView.skin )
			.string_( "dur" )
			.align_( \right );
			
		views[ \dur ] = SMPTEBox( composite, 84@14 )
			.applySmoothSkin
			.applySkin( RoundView.skin )
			.clipLo_(0)
			.action_({ |nb|
				if( nb.value == 0 ) {
					chain.dur_( inf );
				} {
					chain.dur_( nb.value );
				};
			});
			
		views[ \infDur ] = SmoothButton( composite, 25@14 )
			.border_( 1 )
			.radius_( 3 )
			.label_( [ "inf", "inf" ] )
			.hiliteColor_( Color.green )
			.action_({ |bt|
				var dur;
				switch( bt.value, 
					0, { dur = views[ \dur ].value;
						if( dur == 0 ) {
							dur = 1;
						};
						chain.dur_( dur ) },
					1, { chain.dur_( inf ) }
				);
		});
			
		views[ \fromSoundFile ] = SmoothButton( composite, 90@14 )
			.border_( 1 )
			.radius_( 3 )
			.label_( "from soundFile" )
			.action_({ chain.useSndFileDur });
			
		composite.decorator.nextLine;
		
		// fadeTimes
		StaticText( composite, labelWidth@14 )
			.applySkin( RoundView.skin )
			.string_( "fadeTimes" )
			.align_( \right );
		
		views[ \fadeIn ] = SmoothNumberBox( composite, 40@14 )
			.clipLo_(0)
			.scroll_step_(0.1)
			.action_({ |nb|
				chain.fadeIn_( nb.value );
			});
			
		views[ \fadeOut ] = SmoothNumberBox( composite, 40@14 )
			.clipLo_(0)
			.scroll_step_(0.1)
			.action_({ |nb|
				chain.fadeOut_( nb.value );
			});
			
		views[ \releaseSelf ] = SmoothButton( composite, 70@14 )
			.border_( 1 )
			.radius_( 3 )
			.label_( [ "releaseSelf", "releaseSelf" ] )
			.hiliteColor_( Color.green )
			.action_({ |bt|
				chain.releaseSelf = bt.value.booleanValue;
			});
			
		composite.decorator.nextLine;
		
		// gain
		StaticText( composite, labelWidth@14 )
			.applySkin( RoundView.skin )
			.string_( "gain" )
			.align_( \right );
		
		views[ \gain ] = SmoothNumberBox( composite, 40@14 )
			.clipHi_(24) // just to be safe)
			.action_({ |nb|
				chain.setGain( nb.value );
			});
			
		views[ \muted ] = SmoothButton( composite, 40@14 )
			.border_( 1 )
			.radius_( 3 )
			.label_( [ "mute", "mute" ] )
			.hiliteColor_( Color.red )
			.action_({ |bt|
				switch( bt.value, 
					0, { chain.muted = false },
					1, { chain.muted = true }
				);
			});
			
		controller
			.put( \start, { views[ \startButton ].value = 1 } )
			.put( \end, { 
				if( units.every({ |unit| unit.synths.size == 0 }) ) {
					views[ \startButton ].value = 0;
					releaseTask.stop;
				};
			} )
			.put( \gain, { views[ \gain ].value = chain.getGain } )
			.put( \muted, { views[ \muted ].value = chain.muted.binaryValue } )
			.put( \startTime, { views[ \startTime ].value = chain.startTime ? 0; })
			.put( \dur, { var dur;
				dur = chain.dur;
				if( dur == inf ) {
					views[ \dur ].enabled = false; // don't set value
					views[ \infDur ].value = 1;
					views[ \releaseSelf ].hiliteColor = Color.green.alpha_(0.25);
					views[ \releaseSelf ].stringColor = Color.black.alpha_(0.5);
				} {
					views[ \dur ].enabled = true;
					views[ \dur ].value = dur;
					views[ \infDur ].value = 0;
					views[ \releaseSelf ].hiliteColor = Color.green.alpha_(1);
					views[ \releaseSelf ].stringColor = Color.black.alpha_(1);
				};
			})
			.put( \fadeIn, { views[ \fadeIn ].value = chain.fadeIn })
			.put( \fadeOut, { views[ \fadeOut ].value = chain.fadeOut })
			.put( \releaseSelf, {  
				views[ \releaseSelf ].value = chain.releaseSelf.binaryValue;
			})
			.put( \units, { 
				if( composite.isClosed.not ) {
					{
						composite.remove;
						this.makeViews( originalBounds );
					}.defer(0.01);
				};
			})
			.put( \init, { 
				if( composite.isClosed.not ) {
					{
						composite.remove;
						this.makeViews( originalBounds );
					}.defer(0.01);
				};
			});
		
		chain.changed( \gain );
		chain.changed( \mute );
		chain.changed( \startTime );
		chain.changed( \dur );
		chain.changed( \fadeIn );
		chain.changed( \fadeOut );
		chain.changed( \releaseSelf );
		
		uguis = this.makeUnitViews(units, margin, gap );
		
		composite.bounds = composite.bounds.height_( composite.children.last.bounds.bottom );
	}
	
	makeUnitHeader { |units, margin, gap|
		var comp, header, min, io, defs;
		
		comp = CompositeView( composite, (composite.bounds.width - (margin.x * 2))@16 )
			.resize_(2);
		
		header = StaticText( comp, comp.bounds.moveTo(0,0) )
				.applySkin( RoundView.skin )
				.string_( " units" )
				.align_( \left )
				.resize_(2);
		if( chain.class != MassEditUChain ) {
            io = SmoothButton( comp, Rect( comp.bounds.right - (36), 1, 36, 12 ) )
                .label_( "i/o" )
                .border_( 1 )
                .radius_( 2 )
                .action_({
                    var parent;
                    parent = composite.parent;
                    {
                        composite.remove;
                        UChainIOGUI( parent, originalBounds, chain );
                    }.defer(0.01);

                }).resize_(3);
		};
		defs = SmoothButton( comp, 
				Rect( comp.bounds.right - (36 + if( chain.class != MassEditUChain ) {4 + 36}{0}), 1, 36, 12 ) )
			.label_( "defs" )
			.border_( 1 )
			.radius_( 2 )
			.action_({
				UdefListView();
			}).resize_(3);
			
		CompositeView( comp, Rect( 0, 14, (composite.bounds.width - (margin.x * 2)), 2 ) )
			.background_( Color.black.alpha_(0.25) )
			.resize_(2);

	}
	
	makeUnitViews { |units, margin, gap|
		
		var unitInitFunc;
		var comp, header, uview;
		var addLast;
		var ug;
		
		this.makeUnitHeader( units, margin, gap );
		
		unitInitFunc = { |unit, what ...args|
			if( what === \init ) { // close all views and create new
				chain.changed( \units );
			};
		};
		
		if( units.size == 0 ) {
			comp = CompositeView( composite, (composite.bounds.width - (margin.x * 2))@14 )
				.resize_(2);
			
			header = StaticText( comp, comp.bounds.moveTo(0,0) )
				.applySkin( RoundView.skin )
				.string_( " empty: drag unit or Udef here" )
				.background_( Color.yellow.alpha_(0.25) )
				.resize_(2)
				.font_( 
					(RoundView.skin.tryPerform( \at, \font ) ?? 
						{ Font( Font.defaultSansFace, 12) }).boldVariant 
				);
				
			uview = UserView( comp, comp.bounds.moveTo(0,0) );
				
			uview.canReceiveDragHandler_({ |sink|
				var drg;
				drg = View.currentDrag;
				case { drg.isKindOf( Udef ) } 
					{ true }
					{ [ Symbol, String ].includes( drg.class ) }
					{ Udef.all.keys.includes( drg.asSymbol ) }
					{ drg.isKindOf( U ) }
					{ true }
					{ false }
			})
			.receiveDragHandler_({ |sink, x, y|
					case { View.currentDrag.isKindOf( U ) } {
						chain.units = [ View.currentDrag.deepCopy ];
					} { View.currentDrag.isKindOf( Udef ) } {
						chain.units = [ U( View.currentDrag ) ];
					} {   [ Symbol, String ].includes( View.currentDrag.class )  } {
						chain.units = [ U( View.currentDrag.asSymbol ) ];
					};
			})
		};
		
		ug = units.collect({ |unit, i|
			var header, comp, uview, plus, min, defs, io;
			
			comp = CompositeView( composite, (composite.bounds.width - (margin.x * 2))@14 )
				.resize_(2);
			
			header = StaticText( comp, comp.bounds.moveTo(0,0) )
				.applySkin( RoundView.skin )
				.string_( " " ++ i ++ ": " ++ unit.defName )
				.background_( Color.white.alpha_(0.5) )
				.resize_(2)
				.font_( 
					(RoundView.skin.tryPerform( \at, \font ) ?? 
						{ Font( Font.defaultSansFace, 12) }).boldVariant 
				);
			
			
			if( chain.class != MassEditUChain ) {
				
				uview = UserView( comp, comp.bounds.moveTo(0,0) );
				
				uview.canReceiveDragHandler_({ |sink|
					var drg;
					drg = View.currentDrag;
					case { drg.isKindOf( Udef ) } 
						{ true }
						{ [ Symbol, String ].includes( drg.class ) }
						{ Udef.all.keys.includes( drg.asSymbol ) }
						{ drg.isKindOf( U ) }
						{ true }
						{ false }
				})
				.receiveDragHandler_({ |sink, x, y|
					var u;
					case { View.currentDrag.isKindOf( U ) } {
						u = View.currentDrag;

						if( chain.units.includes( u ) ) { 
							chain.units.remove( u ); 
							chain.units = chain.units.insert( i, u );
						} {
							chain.units = chain.units.insert( i, u.deepCopy );
						};
						
					} { View.currentDrag.isKindOf( Udef ) } {
						unit.defName = View.currentDrag;
					} {   [ Symbol, String ].includes( View.currentDrag.class )  } {
						unit.defName = View.currentDrag.asSymbol;
					};
				})
				.beginDragAction_({ 
					unit;
				});
				
				min = SmoothButton( comp, 
							Rect( comp.bounds.right - (12 + 4 + 12), 1, 12, 12 ) )
						.label_( '-' )
						.border_( 1 )
						.action_({
							chain.units = chain.units.select(_ != unit);
						}).resize_(3);
				
				if( units.size == 1 ) {
					min.enabled = false;
				};
				
				plus = SmoothButton( comp, 
						Rect( comp.bounds.right - (12 + 2), 1, 12, 12 ) )
					.label_( '+' )
					.border_( 1 )
					.action_({
						chain.units = chain.units.insert( i, unit.deepCopy );
					}).resize_(3);
					
			};
						
			unit.addDependant( unitInitFunc );
			header.onClose_({ unit.removeDependant( unitInitFunc ) });
			unit.gui( composite, composite.bounds  );
		});
		
		if( units.size > 0 ) {
			addLast = UserView( composite, (composite.bounds.width - (margin.x * 2))@14 )
				.resize_(2);
					
			addLast.canReceiveDragHandler_({ |sink|
					var drg;
					drg = View.currentDrag;
					case { drg.isKindOf( Udef ) } 
						{ true }
						{ [ Symbol, String ].includes( drg.class ) }
						{ Udef.all.keys.includes( drg.asSymbol ) }
						{ drg.isKindOf( U ) }
						{ true }
						{ false }
				})
				.receiveDragHandler_({ |sink, x, y|
						case { View.currentDrag.isKindOf( U ) } {
							chain.units = chain.units ++ [ View.currentDrag.deepCopy ];
						} { View.currentDrag.isKindOf( Udef ) } {
							chain.units = chain.units ++ [ U( View.currentDrag ) ];
						} {   [ Symbol, String ].includes( View.currentDrag.class )  } {
							chain.units = chain.units ++ [ U( View.currentDrag.asSymbol ) ];
						};
				});
		};
		^ug;

	}
	
	remove {
		composite.remove;
	}
	
	close {
		if( composite.isClosed.not ) {
			composite.getParents.last.findWindow.close;
		};
	}
	
	resize_ { |resize| composite.resize_(resize) }
	
	font_ { |font| uguis.do({ |vw| vw.font = font }); }
		
	view { ^composite }
}

+ UChain {
	gui { |parent, bounds| ^UChainGUI( parent, bounds, this ) }
}