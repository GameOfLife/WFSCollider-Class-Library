UChainGUI {
	
	classvar <>skin;
	
	var <chain;
	
	var <parent, <composite, <views, <startButton, <uguis, <controller;
	var <>action;
	
	*initClass {
		StartUp.defer({
			skin = ( 
				labelWidth: 80, 
				font: Font( Font.defaultSansFace, 10 ), 
				hiliteColor: Color.gray(0.33)
			);
		});
	}
	
	*new { |parent, bounds, unit|
		^super.newCopyArgs( unit ).init( parent, bounds );
	}
	
	init { |inParent, bounds|
		parent = inParent;
		if( parent.isNil ) { parent = Window( "UChain" ).front };
		this.makeViews( bounds );
	}
	
	makeViews { |bounds|
		RoundView.useWithSkin( skin ++ (RoundView.skin ? ()), {
			this.prMakeViews( bounds );
		});
	}
	
	prMakeViews { |bounds|
		var margin = 0@0, gap = 4@4;
		var heights, units;
		var labelWidth;
		var unitInitFunc;
		var originalBounds;
		
		labelWidth = 80;
		
		if( RoundView.skin.notNil ) { labelWidth = RoundView.skin.labelWidth ? 80 };
		
		views = ();
		
		originalBounds = bounds.copy;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		units = chain.units.collect({ |u| 
			if( u.class == MetaU ) { u.unit; } { u; }
		});
		bounds.height = units.collect({ |unit|
			UGUI.getHeight( unit, 14, margin, gap ) + 14 + gap.y + gap.y;
		}).sum + (4 * (14 + gap.y));
		
		controller = SimpleController( chain );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove };
		
		// startbutton
		views[ \startButton ] = SmoothButton( composite, 14@14 )
			.label_( ['power', 'power'] )
			.hiliteColor_( Color.green.alpha_(0.5) )
			.action_( [ { chain.prepareAndStart }, { chain.release } ] );
			
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
			.applySkin( RoundView.skin )
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
			
		views[ \fromSoundFile ] = SmoothButton( composite, 60@14 )
			.applySkin( RoundView.skin )
			.border_( 1 )
			.radius_( 3 )
			.label_( "soundFile" )
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
			
		controller
			.put( \start, { views[ \startButton ].value = 1 } )
			.put( \end, { 
				if( units.every({ |unit| unit.synths.size == 0 }) ) {
					views[ \startButton ].value = 0;
				};
			} )
			.put( \gain, { views[ \gain ].value = chain.getGain } )
			.put( \dur, { var dur;
				dur = chain.dur;
				if( dur == inf ) {
					views[ \dur ].enabled = false; // don't set value
					views[ \infDur ].value = 1;
				} {
					views[ \dur ].enabled = true;
					views[ \dur ].value = dur;
					views[ \infDur ].value = 0;
				};
			})
			.put( \fadeIn, { views[ \fadeIn ].value = chain.fadeIn })
			.put( \fadeOut, { views[ \fadeOut ].value = chain.fadeOut });
		
		chain.changed( \gain );
		chain.changed( \dur );
		chain.changed( \fadeIn );
		chain.changed( \fadeOut );
		
		unitInitFunc = { |unit, what ...args|
			if( what === \init ) { // close all views and create new
				composite.remove;
				this.makeViews( originalBounds );
			};
		};
		
		uguis = units.collect({ |unit, i|
			var header;
			
			header = DragSink( composite, (composite.bounds.width - (margin.x * 2))@14 )
				.applySkin( RoundView.skin )
				.string_( " " ++ i ++ ": " ++ unit.defName )
				.background_( Color.white.alpha_(0.5) )
				.resize_(2)
				.font_( 
					(RoundView.skin.tryPerform( \at, \font ) ?? 
						{ Font( Font.defaultSansFace, 12) }).boldVariant 
				);
			
			if( chain.class != MassEditUChain ) {
				header.canReceiveDragHandler_({ |sink|
					var drg;
					drg = View.currentDrag;
					case { drg.isKindOf( Udef ) } 
						{ true }
						{ [ Symbol, String ].includes( drg.class ) }
						{ Udef.all.keys.includes( drg.asSymbol ) }
						{ false }
				})
				.receiveDragHandler_({ |sink, x, y|
					unit.defName = View.currentDrag;
				});
			};
						
			unit.addDependant( unitInitFunc );
			header.onClose_({ unit.removeDependant( unitInitFunc ) });
			unit.gui( composite, composite.bounds  );
		});
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