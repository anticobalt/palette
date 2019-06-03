package iced.egret.palette

class Picture(_name: String, _path: String) : TerminalCoverable {
    override val terminal: Boolean = true
    override val name: String = _name
    override val coverId : Int = R.drawable.ic_folder_silver_24dp
    private val mPath : String = _path
}