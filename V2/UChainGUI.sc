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
		var heights;
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		bounds.height = chain.units.collect({ |unit|
			UGUI.getHeight( unit, 16, margin, gap ) + 16 + gap.y + gap.y;
		}).sum;
		
		controller = SimpleController( chain );
		
		composite = CompositeView( parent, bounds ).resize_(2);
		composite.addFlowLayout( margin, gap );
		composite.onClose = { controller.remove };
		
		startButton = SmoothButton( composite, 16@16 )
			.label_( ['power', 'power'] )
			.hiliteColor_( Color.green.alpha_(0.5) )
			.action_( [ { chain.start }, { chain.release } ] );
			
		
			
		controller
			.put( \start, { startButton.value = 1 } )
			.put( \end, { startButton.value = 0 } );
		
		uguis = chain.units.collect({ |unit, i|
			StaticText( composite, (composite.bounds.width - (margin.x * 2))@16 )
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