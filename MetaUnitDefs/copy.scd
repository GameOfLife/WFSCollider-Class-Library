MetaUdef(\copy,{ |numChannels|
    { |outBus = 0|
        Out.ar(0, UIn.ar(0,numChannels) )
    }
},[\numChannels,1])