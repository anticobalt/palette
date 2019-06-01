package iced.egret.palette

class Picture(val path: String) : Thumbnail {
    override fun getNameTag(): String {
        return path
    }
}