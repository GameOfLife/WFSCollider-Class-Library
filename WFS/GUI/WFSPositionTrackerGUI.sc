WFSPositionTrackerGUI {
	
	classvar <>current;
	
	var <parent, <composite, <startButton, <rateSlider, <view, <controller;
	var <>task;
	
	*new { |parent, bounds|
		^super.newCopyArgs.init( parent, bounds );
	}
	
	*newOrCurrent {
		if( current.notNil && { current.composite.isClosed.not } ) {
			current.composite.getParents.last.findWindow.front;
			^current;
		} {
			^this.new;
		};
	}
	
	init {  |inParent, bounds|
		parent = inParent;
		
		if( parent.isNil ) { 
			parent = "WFSPositionTracker";
		};
		if( parent.class == String ) {
			parent = Window(
				parent, 
				bounds ?? { Rect(128 rrand: 256, 64 rrand: 128, 400, 400) }, 
				scroll: false
			).front;
			this.makeViews( bounds );
			this.makeCurrent;
		} {
			this.makeViews( bounds );
			this.makeCurrent;
		};
		
	}
	
	makeCurrent { current = this; }
	
	makeViews { |bounds|
		
		bounds = bounds ?? { parent.asView.bounds.insetBy(4,4) };
		
		composite = CompositeView( parent, bounds ).resize_(5);
		composite.addFlowLayout( 0@0, 4@4 );
		composite.onClose = { 
			controller.remove; 
			if( current == this ) { current = nil };
			this.stopTask;
			CmdPeriod.remove( this );
		};
		
		startButton = SmoothButton( composite, 18@18 )
			.label_( ['power', 'power'] )
			.radius_(9)
			.border_(1)
			.hiliteColor_( Color.green.alpha_(0.5) )
			.value_( WFSPositionTracker.active.binaryValue )
			.action_({ |bt| 
				switch( bt.value,
					1, { WFSPositionTracker.start },
					0, { WFSPositionTracker.stop }
				); 
			});
		
		rateSlider = EZSmoothSlider( 
				composite, (composite.bounds.width - 22)@18, "rate", [1,100,\exp,1,10].asSpec 
			)
			.value_( WFSPositionTracker.sendPointRate )
			.action_({ |sl|
				WFSPositionTracker.rate = sl.value;
			});
			
		rateSlider.view.resize_(2);
		
		view = WFSMixedView( composite, composite.bounds.insetAll( 0, 22, 0, 0 ), [] )
			.mouseMode_(\lock)
			.showLabels_( false );
			
		controller = SimpleController( WFSPositionTracker )
			.put( \active, {
				if( WFSPositionTracker.active == true ) {
					startButton.value = 1;
					this.startTask;
				} {
					startButton.value = 0;
					this.stopTask;
				};
			})
			.put( \rate, {
				rateSlider.value =  WFSPositionTracker.sendPointRate;
			});
					
		CmdPeriod.add( this );
		
		if( WFSPositionTracker.active ) { this.startTask };
		
	}
	
	update {
		{ 
			if( composite.notNil && { composite.isClosed.not } ) {
				view.objectAndLabels_( *WFSPositionTracker.pointsAndLabels );
			};
		}.defer;
	}
	
	startTask {
		task.stop; // in case it was already running
		task = Task({
			loop {
				this.update;
				(1/WFSPositionTracker.sendPointRate).wait;
			};
		}).start;
	}
	
	stopTask {
		task.stop;
		task = nil;
		this.update;
	}
	
	cmdPeriod {
		if( WFSPositionTracker.active ) { this.startTask; };
	}
	
	
}