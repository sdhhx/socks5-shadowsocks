package cc.litstar.pac;

import java.util.List;

/**
 * @author hehaoxing
 * 存储从socks5pac.json文件中读取到的信息
 */
public class PacList {
	private List<String> regAllowList;
	private List<String> allowList;
	private List<String> blockList;
	public PacList(List<String> regAllowList, List<String> allowList, List<String> blockList) {
		super();
		this.regAllowList = regAllowList;
		this.allowList = allowList;
		this.blockList = blockList;
	}
	public List<String> getRegAllowList() {
		return regAllowList;
	}
	public void setRegAllowList(List<String> regAllowList) {
		this.regAllowList = regAllowList;
	}
	public List<String> getAllowList() {
		return allowList;
	}
	public void setAllowList(List<String> allowList) {
		this.allowList = allowList;
	}
	public List<String> getBlockList() {
		return blockList;
	}
	public void setBlockList(List<String> blockList) {
		this.blockList = blockList;
	}
}
