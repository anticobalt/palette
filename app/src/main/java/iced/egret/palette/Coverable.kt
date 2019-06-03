package iced.egret.palette

interface Coverable {
    val terminal : Boolean
    val name : String
    val coverId : Int
}

interface TerminalCoverable : Coverable {
    override val terminal: Boolean
        get() = true

}