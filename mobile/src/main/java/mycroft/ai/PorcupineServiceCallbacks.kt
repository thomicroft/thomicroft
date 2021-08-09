package mycroft.ai

interface PorcupineServiceCallbacks {
    // TODO hier Interface für Funktionen die PorcupineService von main nutzen möchte

    // TODO diese müsste noch durch Funktion ersetzt werden in der am Ende auch automatisch Aufnahme gestoppt wird
    fun recognizeMicrophone()
    fun showWakeWordToast()

}