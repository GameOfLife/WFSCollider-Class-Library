WFSBatch {

*new {

	var window = Window("Batch",Rect(50, 400, 113, 400),false).front;
	window.addFlowLayout(8@6);
	SmoothButton(window,Rect(10, 30, 90, 20))
		.states_([ [ "Buffer", Color(0.0, 0.0, 0.0, 1.0), Color.grey ] ])
		.canFocus_(false)
		.border_(1)
		.action_{
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.audioType = 'buf' }
		};
	window.view.decorator.nextLine;
	SmoothButton(window,Rect(10, 55, 90, 20))
		.states_([ [ "Disk", Color(0.0, 0.0, 0.0, 1.0), Color.grey ]])
		.canFocus_(false)
		.border_(1)
		.action_{
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.audioType = 'disk' }
		};
	window.view.decorator.nextLine;
	SmoothButton(window,Rect(10, 80, 90, 20))
		.states_([ [ "Linear", Color(0.0, 0.0, 0.0, 1.0), Color.grey ]])
		.canFocus_(false)
		.border_(1)
		.action_{
			WFSScoreEditor.current.selectedEvents.do
				{ |event| [\cubic ].includes( event.wfsSynth.intType ).if({event.wfsSynth.intType = 'linear' }) }
		};
	window.view.decorator.nextLine;
	SmoothButton(window,Rect(10, 105, 90, 20))
		.states_([ [ "Cubic", Color(0.0, 0.0, 0.0, 1.0), Color.grey ]])
		.canFocus_(false)
		.border_(1)
		.action_{
			WFSScoreEditor.current.selectedEvents.do
				{ |event| [\linear ].includes( event.wfsSynth.intType ).if({event.wfsSynth.intType = 'cubic' }) }
		};
	window.view.decorator.nextLine;
	SmoothButton(window,Rect(10, 130, 90, 20))
		.states_([ [ "Swith on", Color(0.0, 0.0, 0.0, 1.0), Color.grey ]])
		.canFocus_(false)
		.border_(1)
		.action_{
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.useSwitch = 'true' }
		};
	window.view.decorator.nextLine;
	SmoothButton(window,Rect(10, 155, 90, 20))
		.states_([ [ "Switch off", Color(0.0, 0.0, 0.0, 1.0), Color.grey ]])
		.canFocus_(false)
		.border_(1)
		.action_{
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.useSwitch = 'false' }
		};
	window.view.decorator.nextLine;
	
	SCStaticText.new(window,Rect(12, 0, 100, 40))
		.string_("Batch Process");
	SCStaticText.new(window,Rect(10, 190, 51, 17))
		.string_("Start pos");
	SCNumberBox.new(window,Rect(60, 190, 40, 15))
		.action_{ |num|
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.startTime = num.value }
		};
	window.view.decorator.nextLine;
	SCStaticText.new(window,Rect(10, 210, 51, 17))
		.string_("Level");
	SCNumberBox.new(window,Rect(60, 210, 40, 15))
		.action_{ |num|
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.level = num.value }
		};
	window.view.decorator.nextLine;
	SCStaticText.new(window,Rect(10, 230, 51, 17))
		.string_("Dur");
	SCNumberBox.new(window,Rect(60, 230, 40, 15))
		.action_{ |num|
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.dur = num.value }
		};
	window.view.decorator.nextLine;
	SCStaticText.new(window,Rect(10, 250, 51, 17))
		.string_("Fade In");
	SCNumberBox.new(window,Rect(60, 250, 40, 15))
		.action_{ |num|
			WFSScoreEditor.current.selectedEvents.do
				{ |event|
				if (event.wfsSynth.fadeTimes.isNil) 
					{ event.wfsSynth.fadeTimes = [num.value,0] }
					{ event.wfsSynth.fadeTimes[0] = num.value }
				}
		};
	window.view.decorator.nextLine;
	SCStaticText.new(window,Rect(10, 270, 51, 17))
		.string_("Fade out");
	SCNumberBox.new(window,Rect(60, 270, 40, 15))
		.action_{ |num|
			WFSScoreEditor.current.selectedEvents.do
				{ |event|
				if (event.wfsSynth.fadeTimes.isNil) 
					{ event.wfsSynth.fadeTimes = [0,num.value] }
					{ event.wfsSynth.fadeTimes[1] = num.value }
				}
		};
	window.view.decorator.nextLine;
	SCStaticText.new(window,Rect(10, 290, 51, 17))
		.string_("StrtFrme");
	SCNumberBox.new(window,Rect(60, 290, 40, 15))
		.action_{ |num|
			WFSScoreEditor.current.selectedEvents.do
				{ |event| event.wfsSynth.startFrame = num.value*44100 }
		};
	window.view.decorator.nextLine;
		
		
	SCStaticText.new(window,Rect(10, 270, 51, 17))
		.string_("x");
	SCNumberBox.new(window,Rect(60, 270, 40, 15))
		.action_{ |num|
			WFSScore.allEvents(WFSScoreEditor.current.selectedEvents)
				.select({ arg ev; ev.wfsSynth.wfsPath.class == WFSPoint })
				.do{ arg ev; ev.wfsSynth.wfsPath.x = num.value }
		};
	window.view.decorator.nextLine;
		
	SCStaticText.new(window,Rect(10, 270, 51, 17))
		.string_("y");
	SCNumberBox.new(window,Rect(60, 270, 40, 15))
		.action_{ |num|
			WFSScore.allEvents(WFSScoreEditor.current.selectedEvents)
				.select({ arg ev; ev.wfsSynth.wfsPath.class == WFSPoint })
				.do{ arg ev; ev.wfsSynth.wfsPath.y = num.value }
		};
	window.view.decorator.nextLine;



	}
}
	
