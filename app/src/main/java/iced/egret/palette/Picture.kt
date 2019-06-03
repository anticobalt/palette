package iced.egret.palette

class Picture(override val name: String, path: String) : TerminalCoverable {
    override val terminal: Boolean = true
    override val coverId : Int = R.drawable.ic_folder_silver_24dp
    private val mPath : String = path
}