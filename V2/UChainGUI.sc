UChainGUI {
	
	var <chain;
	
	var <parent, <composite, <startButton, <uguis, <controller;
	var <>action;
	
	*new { |parent, bounds, unit|
		^super.newCopyArgs( unit ).init( parent, bounds );
	}
	
	init { |inParent, bounds|
		parent = inParent;
		if( parent.isNil ) { parent = Window( "UChain" ).front };
		this.makeViews( bounds );
	}
	
	makeViews { |bounds|
		var margin = 0@0, gap = 4@4;
		var heights, units;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		units = chain.units.collect({ |u| 
			if( u.class == MetaU ) { u.unit; } { u; }
		});
		bounds.height = units.collect({ |unit|
			UGUI.getHeight( unit, 14, margin, gap ) + 14 + gap.y + gap.y;
		}).sum + 14 + gap.y;
		
		controller = SimpleController( chain );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove };
		
		startButton = SmoothButton( composite, 14@14 )
			.label_( ['power', 'power'] )
			.hiliteColor_( Color.green.alpha_(0.5) )
			.action_( [ { chain.prepareAndStart }, { chain.release } ] );
			
		controller
			.put( \start, { startButton.value = 1 } )
			.put( \end, { startButton.value = 0 } );
		
		uguis = units.collect({ |unit, i|
			StaticText( composite, (composite.bounds.width - (margin.x * 2))@14 )
				.applySkin( RoundView.skin )
				.string_( " " ++ i ++ ": " ++ unit.defName )
				.background_( Color.white.alpha_(0.5) )
				.resize_(2)
				.font_( 
					(RoundView.skin.tryPerform( \at, \font ) ?? 
						{ Font( Font.defaultSansFace, 12) }).boldVariant 
				);
			unit.gui( composite, composite.bounds  );
		});
	}
	
	resize_ { |resize| composite.resize_(resize) }
	
	font_ { |font| uguis.do({ |vw| vw.font = font }); }
		
	view { ^composite }
}

+ UChain {
	gui { |parent, bounds| ^UChainGUI( parent, bounds, this ) }
}