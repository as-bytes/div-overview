<?xml version="1.0" encoding="UTF-8"?>
<!-- DUSK
bg: 1A232E, 171D25
-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:xdt="http://www.w3.org/2005/xpath-datatypes"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns="http://www.w3.org/1999/xhtml">

	<xsl:variable name="parserTypes" as="element()*">
<!--		<Item id="INGDIBA" name="IngDiBa" />-->
<!--		<Item id="TRADEREPUBLIC" name="TradeRepublic" />-->
<!--		<Item id="DEGIRO" name="DeGiro" />-->
<!--		<Item id="FINNETZERO_PDF" name="Finanzen.Net Zero (PDF)" />-->
		<Item id="FINNETZERO_CSV" name="Finanzen.Net Zero (CSV)" />
<!--		<Item id="SMARTBROKER" name="SmartBroker" />-->
<!--		<Item id="QUIRION" name="Quirion" />-->
<!--		<Item id="BITPANDA" name="Bitpanda (Stocks)" />-->
<!--		<Item id="VIVID" name="Vivid Money" />-->
		<Item id="SCALABLE" name="Scalable Baader" />
		<Item id="SCALABLE2" name="Scalable Capital" />
	</xsl:variable>

	<xsl:variable name="years" as="element()*">
		<Item id="2025" />
		<Item id="2024" />
		<Item id="2023" />
		<Item id="2022" />
		<Item id="2021" />
<!--		<Item id="2020" />-->
<!--		<Item id="2019" />-->
<!--		<Item id="2018" />-->
<!--		<Item id="2017" />-->
<!--		<Item id="2016" />-->
<!--		<Item id="2015" />-->
<!--		<Item id="2014" />-->
<!--		<Item id="2013" />-->
	</xsl:variable>

	<xsl:output method="html" version="4.0" indent="yes" />
	<xsl:decimal-format name="d" decimal-separator="," grouping-separator="."/>
	<xsl:template match="Shares">
		<html>
			<head>
				<!--suppress CssUnusedSymbol -->
				<style type="text/css">
					#byMonth tr:not(.current-year) {
						opacity: 0.6;
					}

					#byMonth .current-year {
						height: 40px;
					}

					table a[title^='ING'] {
						color: #d07d00;
					}

					table a[title^='TRA'] {
						color: #b9c500;
					}

					table a[title^='DEG'] {
						color: #00adff;
					}

					table a[title^='SMA'] {
						color: #149945;
					}

					table a[title^='VIV'] {
						color: #7d33f6;
					}

					table a[title^='BITP'] {
						color: #08f596;
					}

					table a[title^='FINN'] {
						color: #f8a46c;
					}

					table a[title^='SC'] {
						color: #14bebf;
					}

					button {
						font-size: 8pt;
						border: 1px solid black;
						margin: 2px;
						cursor: pointer;
						background-color: #4c4c4c;
						color: #c8c5c5;
					}

					/** COLORS **/
					.summaryseparator {
						border: 0;
						background-color: #727272;
					}

					.bgblack {
						background-color: black;
					}

					.bgslate {
						background-color: darkslategray;
					}

					.bg30 {
						background-color: #303030;
						color: white;
						font-weight: bold;
					}

					.bgtransparent {
						background: transparent !important;
					}

					.noborders {
						border: none;
					}

					body, .blank {
						font-size: 9pt;
						background-color: #595959;
						width: 1000px;
						margin: auto;
					}

					th {
						background-color: #303030;
						color: white;
					}

					tr:not(.all), tr[name=details] {
						background-color: #424242;
						color: #dbdbdb;
					}

					a {
						color: #dbdbdb;
					}

					table#summary > tbody > tr#summaries > td {
						background-color: #727272;
						border: 1px solid black;
					}

					table#summary > tbody > tr#summaries table.summaries tr.annual-gesamt {
						background-color: #303030;
					}

					/* table.summaries tbody tr td:not([colspan]) { background-color: #424242!important; } */

					table#summary > thead#overview > td {
						background-color: #4b4b4b;
					}


					table#byDate tbody tr:hover *, table#byIsinOpen tbody tr:hover *, table#byIsinClosed tbody tr:hover *, button:hover {
						background-color: yellow !important;
						color: black !important;
					}

					table#byIsinOpen tbody tr.all > td {
						cursor: pointer;
					}

					table#byIsinOpen tbody tr[name=details] > td:first-child {
						cursor: pointer;
					}

					table#byIsinOpen tr[name=details] td, table#byIsinClosed tr[name=details] td {
						background-color: #2e2e2e;
					}

					table#byIsinOpen tbody tr.all, table#byIsinClosed tbody tr.all {
						background-color: #424242 !important;
						color: white;
					}

					.sum {
						border-left: 5px solid yellow;
						border-right: 5px solid yellow;
					}

					a:hover {
						text-decoration: underline;
					}

					th, td {
						font-size: 8pt;
					}

					*[align=left], .left {
						text-align: left !important;
						padding-left: 5px;
					}
					*[align=right], .right {
						text-align: right !important;
						padding-right: 5px;
					}

					*[align=center], .center {
						text-align: center !important;
					}

					/** CELL COLORS **/
					span[title^='+'], td[title^='+'], *[title^='SELL'] {
						color: #2cdc2c;
					}

					*[title^='SELL-TMP'], *[title^='BUY-TMP'], *[title^='BUYSPLIT'], *[title^='SELLSPLIT'] {
						color: #f8f800;
					}

					span[title^='-'], td[title^='-'], *[title^='BUY'], *[title^='KNO'], *[title^='INTEREST'], *[title^='FEE'], *[title^='TAX'], *[title^='TAX'] ~ td {
						color: #f83a3a;
					}

					*[title^='DIV'], *[title^='DIV'] ~ td {
						color: #6565ff;
					}

					td[title^='SELL'], *[title^='SELL'] ~ td {
						border-bottom-color: #2cdc2c;
					}

					td[title^='BUY'], *[title^='BUY'] ~ td {
						border-bottom-color: #f83a3a;
					}

					td[title^='DIV'], *[title^='DIV'] ~ td {
						border-bottom-color: #6565ff;
						border-style: dashed;
					}

					td[title^='+0,00'], td[title^='-,--'], td[title^='--.--.----'] {
						color: silver;
					}

					span[title^='+0,00'], span[title^='-,--'], span[title^='--.--.----'] {
						color: silver;
					}

					table#byIsinOpen tr[name='details'] td[name='type'][title^='BUY'] + td + td + td + td {
						color: silver;
					}

					/** NEGATIVE OPEN AMOUNT **/
					tr.all td:nth-child(2)[title^='-'] {
						background-color: yellow;
					}


					/** SUMMARY HEADER **/
					thead#overview td {
						text-align: center;
					}

					table {
						margin: auto;
						width: 100%;
						padding: 15px 20px 10px 10px;
					}

					tbody tr[disabled='true'] {
						text-decoration: line-through;
					}

					tbody tr[disabled='true']:first-child {
						border-top: 2px solid black;
						margin-top: 10px;
					}

					tbody tr[disabled='true']:nth-child(1) {
						border-top: 20px solid black;
					}

					tbody tr.all {
						cursor: pointer;
					}

					tbody tr[name=details] td:first-child {
						text-align: center !important;
					}

					tr[name=details] tr[name=type], table#byIsinOpen th[name=type], table#byIsinClosed th[name=type] {
						width: 90px;
					}

					.hidden {
						display: none;
					}

					.show {
						display: block;
					}

					td .isin {
						float: right;
					}

					th, td {
						border: 0.1px solid black;
						font-family: Consolas, monospace;
					}

					th {
						padding: 3px;
					}

					thead tr.all td:nth-child(1) {
						text-align: center;
					}

					thead tr.all td {
						text-align: right;
						padding: 0 15px 0 15px
					}

					/* tbody tr.all td { border-bottom: 1px dashed black; } */


					tbody tr.all a, td[colspan] div {
						white-space: nowrap;
						text-overflow: ellipsis;
						word-break: keep-all;
						overflow: hidden;
						display: block;
					}

					tbody tr.all a { /*display: block;*/
						width: 200px;
					}

					tr.all td:first-child a {
						text-decoration: none;
					}

					tr.all td:first-child a:hover {
						text-decoration: underline;
					}

					table:not(.summaries) > tbody > tr.all > td:not([colspan]) {
						padding: 0 5px 0 5px;
					}

					tr.all td[colspan] {
						padding: 0 5px 0 5px;
					}

					tr.all td[colspan] {
						text-align: left;
					}

					/** BLANK LINE **/
					.blank {
						border: none;
						display: none;
					}

					/* .blank:before { content: '\00a0'; } */

					/** CHECKED OPEN AMOUNT BOXES **/
					input[type='checkbox'] {
						float: left;
						cursor: pointer;
						box-shadow: inset 0 0 0 10px red;
					}

					input[type="checkbox"]:checked {
						box-shadow: inset 0 0 0 10px #00ff00;
					}

					/** BY ISIN **/
					.invmid {
						background-color: #343434;
					}

					.invhigh {
						background-color: #242424;
					}

					/** Ellipsis **/
					table#byDate > tbody > tr > td:nth-child(2) {
						text-overflow: ellipsis;
						white-space: nowrap;
						max-width: 250px;
						overflow: hidden;
					}

					/** SUMMARIES **/
					.summaries {
						width: 940px!important;
						display:block;
						overflow-x: scroll;
						padding: 10px 0 10px 0;
					}

					.summaries > thead > th {
						min-width: 225px!important;
					}

					.summaries > thead > td {
						text-align: center !important;
					}

					.summaries > thead .month {
						font-size: 9pt;
						font-weight: bold;
						border-bottom: 1px solid black;
						white-space: nowrap;
						text-align: center;
					}

					.summaries > tbody > td:nth-child(1) {
						text-align: right !important;
					}

					.summaries > tbody > td:nth-child(2) {
						text-align: left !important;
					}

					.summaries > tbody > td {
						font-size: 8pt;
						width: 45px;
						padding-left: 5px;
						padding-right: 5px;
					}

					/***************/


				</style>
				<script>
					function loaded() {
						document.getElementsByName('details').forEach((tr) => {
							tr.className = 'hidden';
							if (tr.previousElementSibling.className === 'all') {
								tr.previousElementSibling.onclick = toggleMe;
							}
							tr.firstElementChild.onclick = markForSum;
						});

						for (tr of document.getElementById('byDate').children[1].children) {
							if (tr.children.length > 6) {
								tr.children[6].onclick = sumMe;
								tr.children[6].style.cursor = 'pointer';
							}
						}

						activateSummaryFolding();
					}

					function markForSum(evt) {
						let marker = " sum";
						let isin = evt.currentTarget.parentElement.dataset.isin;
						if (evt.currentTarget.className.indexOf(marker) > -1) {
							evt.currentTarget.className = evt.currentTarget.className.replace(marker, "").trim();
						} else {
							evt.currentTarget.className = evt.currentTarget.className.trim() + marker;
						}
						updateSum();
					}

					function sumMe(evt) {
						let marker = " sum sumByDate";
						if (evt.currentTarget.className.indexOf(marker) > -1) {
							evt.currentTarget.className = evt.currentTarget.className.replace(marker, "").trim();
						} else {
							evt.currentTarget.className = evt.currentTarget.className.trim() + marker;
						}
						updateByDateSum();
					}

					function updateByDateSum() {
						let winloss = 0.0;
						let sums = document.getElementsByClassName("sumByDate");
						[...sums].forEach((td) => {
							winloss += cur2dec(td.innerText);
						});

						let html = "";
						if (sums.length > 0) {
							html += "Win/Loss: " + dec2cur(winloss);
						}

						let dateTable = document.getElementById("byDate");
						let byDateSum = document.getElementById('byDateSum');
						if (!byDateSum) {
							byDateSum = document.createElement('div');
							byDateSum.id = 'byDateSum';
							byDateSum.className = 'center bg30';
							byDateSum.style.fontFamily = 'Consolas';
							document.body.insertBefore(byDateSum, dateTable);
						}

						byDateSum.innerHTML = html;
					}

					function updateSum() {
						let idxTotal = 4;
						let idxWinLoss = 5;
						let invested = 0.0;
						let total = 0.0;
						let winloss = 0.0;
						let sums = document.getElementsByClassName("sum");
						[...sums].forEach((td) => {
							if (td.innerText === "BUY") {
								invested += cur2dec(td.parentElement.children[idxTotal].innerText);
							}
							total += cur2dec(td.parentElement.children[idxTotal].innerText);
							winloss += cur2dec(td.parentElement.children[idxWinLoss].innerText);
						});

						let html = "";
						if (sums.length > 0) {
							html += "Selected: " + sums.length;
							html += ", Invested: " + dec2cur(invested);
							html += ", Total: " + dec2cur(total);
							html += ", Win/Loss: " + dec2cur(winloss);
							html += " &lt;a href='javascript: resetSum();'>X&lt;/a>";
						}
						document.getElementById("sumtext").innerHTML = html;
					}

					function resetSum() {
						let sums = document.getElementsByClassName("sum");
						[...sums].forEach((td) => {
							td.click();
						});
						updateSum();
					}

					function cur2dec(cur) {
						return Number(cur.replace('.', '').replace(',','.').replace("€", "").trim());
					}


					function dec2cur(dec) {
						return new Intl.NumberFormat('de-DE', { style: 'decimal', minimumFractionDigits: '2', maximumFractionDigits: 2 }).format(dec);
					}

					function toggleMe(evt) {
						if (evt.target.tagName !== 'A') {
<!--						if (evt.path[0].tagName !== 'A') {-->
							let isin = evt.currentTarget.dataset.isin;
							document.getElementsByName('details').forEach((tr) => {
								if (tr.dataset.isin === isin) {
									toggleHide(tr);
								}
							});
						}
					}

					function toggleHide(cell) {
						cell.className = (cell.className === '') ? 'hidden' : '';
					}

					window.onload = function () {
						addCheckboxes();
					}

					function activateSummaryFolding() {
						// let summaries = document.querySelectorAll("table.summaries thead th");
					}

					function addCheckboxes() {
						let rows = document.querySelectorAll("#byDate tbody tr");
						rows.forEach((row) => {
							let isin = row.children[2].innerText;
							let newHtml = "&lt;input type='checkbox' class='sum' id='" + isin + "'&gt;";
							newHtml += "&lt;label type='checkbox' for='" + isin + "'&gt;";
							newHtml += row.children[0].innerHTML;
							newHtml += "&lt;/label&gt;";
							row.children[0].innerHTML = newHtml;
						});
					}

					function toggleParser(cell) {
    					const parserId = cell.id;
    					const isActive = cell.checked === true || cell.checked === "checked";
    					const tableByDate = document.getElementById('byDate');
						const rows = tableByDate.getElementsByClassName(parserId);
						for (let row of rows) {
							row.style.display = (isActive) ? 'table-row' : 'none';
						}
					}

					function toggleType(cell) {
    					const typeName = cell.id;
    					const isActive = cell.checked === true || cell.checked === "checked";
    					const tableByDate = document.getElementById('byDate');
						const rows = tableByDate.getElementsByClassName(typeName);
						for (let row of rows) {
							row.style.display = (isActive) ? 'table-row' : 'none';
						}
					}
				</script>
			</head>
			<body onload='loaded();'>
				<xsl:variable name="openTotal" select="format-number(@openTotal, '+###.##0,00;-#', 'd')" />
				<xsl:variable name="winloss" select="format-number(@winloss, '+###.##0,00;-#', 'd')" />
				<xsl:variable name="dividend" select="format-number(@dividend, '+###.##0,00;-#', 'd')" />
				<!-- TODO add crypto tax (on total winloss) -->
				<xsl:variable name="tax" select="format-number(@tax, '+###.##0,00;-#', 'd')" />
				<xsl:variable name="fee" select="format-number(@fee, '+###.##0,00;-#', 'd')" />
				<xsl:variable name="interest" select="format-number(@interest, '+###.##0,00;-#', 'd')" />
				<xsl:variable name="year" select="substring(@date, 1, 4)" />
<!--				<xsl:variable name="marketval" select="substring(@date, 0, 5)" />-->
				<table id='summary'>
					<thead id="overview">
						<tr>
							<th>Type</th><th>Invested</th><th>Win/Loss</th>
							<th>Tax</th><th>Fee</th><th>Interest</th>
							<!--<th>MarketVal</th>-->
						</tr>
						<tr id='partialoverview' class='hidden'> </tr>
						<tr id='totaloverview'>
							<td>∑</td>
							<td>
								<xsl:attribute name="title">
									<xsl:value-of select="$openTotal" />
								</xsl:attribute>
								<xsl:value-of select="$openTotal" />
							</td>
							<td>
								<xsl:attribute name="title">
									<xsl:value-of select="$winloss" />
									<xsl:text>&#xA;Divs: </xsl:text>
									<xsl:value-of select="$dividend" />
								</xsl:attribute>
								<xsl:value-of select="$winloss" />
							</td>
							<td>
								<xsl:attribute name="title">
									<xsl:value-of select="$tax" />
								</xsl:attribute>
								<xsl:value-of select="$tax" />
							</td>
							<td>
								<xsl:attribute name="title">
									<xsl:value-of select="$fee" />
								</xsl:attribute>
								<xsl:value-of select="$fee" />
							</td>
							<td>
								<xsl:attribute name="title">
									<xsl:value-of select="$interest" />
								</xsl:attribute>
								<xsl:value-of select="$interest" />
							</td>
<!--							<td>-->
<!--								<xsl:attribute name="title">-->
<!--									<xsl:value-of select="$marketval" />-->
<!--								</xsl:attribute>-->
<!--								<xsl:value-of select="$marketval" />-->
<!--							</td>-->
						</tr>
					</thead>
					<tbody>
						<tr id="summaries">
						<td colspan="6">
						<table class='summaries'>
							<thead>
								<tr>
									<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
										<xsl:variable name="yearId" select="@id"/>
										<th colspan="2"><span class='month'>
											<xsl:value-of select="$yearId" />
										</span></th>
									</xsl:for-each>

								</tr>
							</thead>
							<tbody>
								<xsl:for-each select="document('')/*/xsl:variable[@name='parserTypes']/*">
									<xsl:variable name="parserId" select="@id"/>
									<xsl:variable name="parserName" select="@name"/>
									<tr>
										<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
											<xsl:variable name="yearId" select="@id"/>
											<xsl:variable name="parserSum" select="sum(//share/*[contains(@date, $yearId)][@parserType=$parserId]/@winloss)" />
											<td colspan="2">
												<div style="color: #aaffaa; width: 50%; float:left; clear:both">
													<xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)][@parserType=$parserId][@winloss &gt;=0]/@winloss), '+###.##0;-#', 'd')" />
												</div>
												<div style="color: #ffaaaa; width: 50%; text-align: end;">
													<xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)][@parserType=$parserId][@winloss &lt; 0]/@winloss), '###.##0;-#', 'd')" />
												</div>
												<div style="width: 100%; text-align: center; line-height: 16px">
													<u><xsl:value-of select="$parserName" /></u>
													<br/>
													<xsl:value-of select="format-number($parserSum, '+###.##0;-#', 'd')" />
												</div>
											</td>

<!--											<td align="right">-->
<!--												<span style="color: #aaffaa;">-->
<!--													<xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)][@parserType=$parserId][@winloss &gt;=0]/@winloss), '+###.##0;-#', 'd')" />-->
<!--												</span>-->
<!--												<br />-->
<!--												<span style="color: #ffaaaa;">-->
<!--													<xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)][@parserType=$parserId][@winloss &lt; 0]/@winloss), '###.##0;-#', 'd')" />-->
<!--												</span>-->
<!--												<br />-->
<!--													<xsl:value-of select="format-number($parserSum, '+###.##0;-#', 'd')" />-->
<!--											</td>-->
<!--											<td>-->
<!--												<xsl:value-of select="$parserName" />-->
<!--											</td>-->

<!--											<td>-->
<!--												<table width="100%">-->
<!--													<tr>-->
<!--														<td align="left" style="color: #aaffaa; text-align: left;">-->
<!--															<xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)][@parserType=$parserId][@winloss &gt;=0]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--														</td>-->
<!--														<td align="right" style="color: #ffaaaa; text-align: right;">-->
<!--															<xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)][@parserType=$parserId][@winloss &lt; 0]/@winloss), '###.##0,00;-#', 'd')" />-->
<!--														</td>-->
<!--													</tr>-->
<!--													<tr>-->
<!--														<td colspan="2" align="center" style="border-bottom: 1px solid white; text-align: center;">-->
<!--															<xsl:value-of select="format-number($parserSum, '-###.##0,00;-#', 'd')" />-->
<!--														</td>-->
<!--													</tr>-->
<!--												</table>-->
<!--											</td>-->
<!--											<td>-->
<!--												<xsl:value-of select="$parserName" />-->
<!--											</td>-->
										</xsl:for-each>

<!--										<td align="right">-->
<!--											<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2020')][@parserType=$parserId]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--										</td><td><xsl:value-of select="$parserName" /></td>-->
<!--										<td align="right">-->
<!--											<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2019')][@parserType=$parserId]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--										</td><td><xsl:value-of select="$parserName" /></td>-->
<!--										<td align="right">-->
<!--											<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2018')][@parserType=$parserId]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--										</td><td><xsl:value-of select="$parserName" /></td>-->
									</tr>

								</xsl:for-each>

								<!-- TODO differentiate between taxed and not -->
<!--								<tr>-->
<!--									<td align="right">-->
<!--										<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2021')][@parserType='BISON']/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--									</td><td>Bison [Untaxed]</td>-->
<!--									<td align="right">-->
<!--										<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2020')][@parserType='BISON']/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--									</td><td>Bison [Untaxed]</td>-->
<!--									<td align="right">-->
<!--										<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2019')][@parserType='BISON']/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--									</td><td>Bison [Untaxed]</td>-->
<!--									<td align="right">-->
<!--										<xsl:value-of select="format-number(sum(//share/*[contains(@date, '2018')][@parserType='BISON']/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--									</td><td>Bison [Untaxed]</td>-->
<!--								</tr>-->

								<tr style="font-weight: bold;" class="annual-gesamt">
									<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
									<xsl:variable name="yearId" select="@id"/>
										<xsl:variable name="winlossOfYear" select="sum(//share/*[contains(@date, $yearId)]/@winloss)" />
										<xsl:variable name="winlossOfYearFormatted" select="format-number($winlossOfYear, '+###.##0,00;-#', 'd')" />
										<td style="width: 60px" align="right">Gesamt:</td>
										<td align="right">
											<xsl:attribute name="title">
												<xsl:value-of select="$winlossOfYearFormatted"/>
											</xsl:attribute>
											<xsl:value-of select="$winlossOfYearFormatted" />
										</td>
									</xsl:for-each>
								</tr>
							<tr>
               					<td colspan="6" class='summaryseparator'>&#160;</td>
							</tr>
							<tr>
								<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
									<xsl:variable name="yearId" select="@id"/>
									<xsl:variable name="winlossOfYear" select="sum(//share/*[contains(@date, $yearId)]/@winloss)" />

									<td align="right">
										<xsl:value-of select="format-number(sum(//share/div[contains(@date, $yearId)]/@winloss), '+###.##0,00;-#', 'd')" />
									</td>
									<td>
										Dividend
										<xsl:variable name="absOfYear" select="number(translate($winlossOfYear, '-', ''))" />
										(<xsl:value-of select="format-number((sum(//share/div[contains(@date, $yearId)]/@winloss) div $absOfYear), '##0,00%', 'd')" />)
									</td>
								</xsl:for-each>
<!--								-->
<!--								<td align="right">-->
<!--								    <xsl:value-of select="format-number(sum(//share/div[contains(@date, '2020')]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--								</td>-->
<!--								<td>-->
<!--									Dividend-->
<!--									<xsl:variable name="abs2020" select="number(translate($winloss2020, '-', ''))" />-->
<!--									(<xsl:value-of select="format-number((sum(//share/div[contains(@date, '2020')]/@winloss) div $abs2020), '##0,00%', 'd')" />)-->
<!--								</td>-->

<!--								<td align="right">-->
<!--									<xsl:value-of select="format-number(sum(//share/div[contains(@date, '2019')]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--								</td>-->
<!--								<td>-->
<!--									Dividend-->
<!--									<xsl:variable name="abs2019" select="number(translate($winloss2019, '-', ''))" />-->
<!--									(<xsl:value-of select="format-number((sum(//share/div[contains(@date, '2019')]/@winloss) div $abs2019), '##0,00%', 'd')" />)-->
<!--								</td>-->

<!--								<td align="right">-->
<!--									<xsl:value-of select="format-number(sum(//share/div[contains(@date, '2018')]/@winloss), '+###.##0,00;-#', 'd')" />-->
<!--								</td>-->
<!--								<td>-->
<!--									Dividend-->
<!--									<xsl:variable name="abs2018" select="number(translate($winloss2018, '-', ''))" />-->
<!--									(<xsl:value-of select="format-number((sum(//share/div[contains(@date, '2018')]/@winloss) div $abs2018), '##0,00%', 'd')" />)-->
<!--								</td>-->
							</tr>
								<tr>
									<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
									<xsl:variable name="yearId" select="@id"/>
										<td align="right"><xsl:value-of select="count(//share/*[contains(@date, $yearId)])" /></td>
										<td>Trades</td>
									</xsl:for-each>
								</tr>
								<tr>
									<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
									<xsl:variable name="yearId" select="@id"/>
										<td align="right"><xsl:value-of select="format-number(sum(//share/*[contains(@date, $yearId)]/@fee | //share/*[contains(@date, $yearId)]/@tax), '+###.##0,00;-#', 'd')" /></td>
										<td>Fees/Taxes</td>
									</xsl:for-each>
								</tr>
							</tbody>
						</table>
						<!-- xsl:for-each select="summaries/entry">
							<xsl:sort select="key" order="descending" />
							<td>
							<span class='month'><xsl:value-of select="key" /></span>
							<br/>
							<span class='gain'>
								<xsl:value-of select="format-number(value, '+###.##0,00;-#', 'd')" />
							</span>
							</td>
						</xsl:for-each -->
					</td>
					</tr>

					<tr>
						<td colspan="5" align='left'>
							<button onclick="toggleHide(document.getElementById('byDate'));">By Date</button>
							<button onclick="toggleHide(document.getElementById('byMonth'));">By Month</button>
							<button onclick="toggleHide(document.getElementById('byIsinOpen'));">By ISIN Open</button>
							<button onclick="toggleHide(document.getElementById('byDividends'));">By Dividends</button>
							<button onclick="toggleHide(document.getElementById('byIsinClosed'));">By ISIN Closed</button>
						</td>
						<td colspan="1" align='center'>
							<xsl:value-of select="@date"/>
						</td>
					</tr>
					</tbody>
				</table>

				<table id="byMonth">
					<xsl:for-each select="document('')/*/xsl:variable[@name='years']/*">
					<xsl:variable name="yearId" select="@id"/>
					<tr>
						<xsl:if test="$yearId = $year">
							<xsl:attribute name="class">current-year</xsl:attribute>
						</xsl:if>
						<xsl:if test="not($yearId = $year)">
							<xsl:attribute name="class">prev-year hidden</xsl:attribute>
						</xsl:if>
						<xsl:call-template name="MonTemplate">
							<xsl:with-param name="yearNum" select="$yearId" />
						</xsl:call-template>
					</tr>
					</xsl:for-each>
				</table>

				<table>
					<tr>
						<xsl:for-each select="document('')/*/xsl:variable[@name='parserTypes']/*">
							<xsl:variable name="parserId" select="@id"/>
							<xsl:variable name="parserName" select="@name"/>
							<td>
								<input type="checkbox" onClick="javascript: toggleParser(this)" checked="checked">
									<xsl:attribute name="id"><xsl:value-of select="$parserId" /></xsl:attribute>
									<xsl:value-of select="$parserName" /></input>
							</td>
						</xsl:for-each>
					</tr>
				</table>

				<table>
					<tr>
						<td>
							<input type="checkbox" onClick="javascript: toggleType(this)" checked="checked">
								<xsl:attribute name="id">BUY</xsl:attribute>BUY</input>
						</td>
						<td>
							<input type="checkbox" onClick="javascript: toggleType(this)" checked="checked">
								<xsl:attribute name="id">SELL</xsl:attribute>SELL</input>
						</td>
						<td>
							<input type="checkbox" onClick="javascript: toggleType(this)" checked="checked">
								<xsl:attribute name="id">DIV</xsl:attribute>DIV</input>
						</td>
						<td>
							<input type="checkbox" onClick="javascript: toggleType(this)" checked="checked">
								<xsl:attribute name="id">FEE</xsl:attribute>FEE</input>
						</td>
						<td>
							<input type="checkbox" onClick="javascript: toggleType(this)" checked="checked">
								<xsl:attribute name="id">INTEREST</xsl:attribute>INTEREST</input>
						</td>
						<td>
							<input type="checkbox" onClick="javascript: toggleType(this)" checked="checked">
								<xsl:attribute name="id">TAX</xsl:attribute>TAX</input>
						</td>
					</tr>
				</table>

				<table id="byDate">
					<thead>
					<tr>
						<th>Date</th>
						<th class="left">Name</th>
						<th>Isin</th>
						<th>Amount</th>
						<th>Rate</th>
						<th>Total</th>
						<th>Win/Loss<br/>(FIFO)</th>
						<th>Win/Loss<br/>(AVG)</th>
						<th>%</th>
						<th>Tax</th>
						<th>Fee</th>
						<th>Interest</th>
					</tr>
					</thead>
					<tbody>
						<xsl:call-template name="CwTemplate" />
					</tbody>
				</table>

				<table id="byIsinOpen" class='hidden'>
					<thead>
						<tr><th name="type">Type</th><th>Date</th><th>Amount</th><th>Rate</th><th>Total</th><th>Win/Loss</th><th>MarketVal</th><th>Tax</th><th>Fee</th>
						</tr>
					</thead>
					<tbody>
						<xsl:for-each select="share[@closed='false']">
						<!-- tr><td class="blank" colspan="8"></td></tr -->
						<tr class="all">
							<xsl:attribute name="disabled">
								<xsl:value-of select="@closed" />
							</xsl:attribute>
							<xsl:attribute name="data-isin">
								<xsl:value-of select="@isin" />
							</xsl:attribute>
							<td colspan="1">
								<xsl:attribute name="title">
									<xsl:value-of select="@name" />&#160;(<xsl:value-of select="@isin" />)
								</xsl:attribute>
								<div>
									+ <xsl:value-of select="@name" />
								</div>
							</td>
							<td>
								<a target="_blank">
									<xsl:attribute name="href">http://www.google.de?q=<xsl:value-of select="@isin" /></xsl:attribute>
									<xsl:attribute name="id"><xsl:value-of select="@isin" /></xsl:attribute>
									<xsl:value-of select="@isin" />
								</a>
							</td>
							<!-- Share Col 2: Open Amount -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(sum(./*/@openAmount), '###.##0,000;#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@openAmount), '###.##0,000;#', 'd')" />
							</td>
							<!-- Share Col 3: Open Amount Rate -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(@rate, '+###.##0,000;-#', 'd')" />&#160;<xsl:value-of select="@currency" />
								</xsl:attribute>
								<xsl:value-of select="format-number(@rate, '+###.##0,000;-#', 'd')" />&#160;<xsl:value-of select="@currency" />
							</td>
							<!-- Share Col 4: Open Invested -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(@openTotal, '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:attribute name="class">
									<xsl:if test="@openTotal &gt; -10000 and @openTotal &lt; -5000">invmid</xsl:if>
									<xsl:if test="@openTotal &lt; -10000">invhigh</xsl:if>
								</xsl:attribute>
								<xsl:value-of select="format-number(@openTotal, '+###.##0,00;-#', 'd')" />
							</td>
							<!-- Share Col 5: Win/Loss -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(sum(./*/@winlossNoTax), '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@winlossNoTax), '+###.##0,00;-#', 'd')" />
							</td>
							<!-- Share Col: Market Value -->
							<td align="right" class="bgslate">
								<xsl:if test="@bid &gt; 0">
									<xsl:variable name="oAmt" select="sum(./*[@openAmount]/@openAmount)" />
									<xsl:variable name="mVal" select="format-number(($oAmt * @bid + @openTotal), '+###.##0,00;-#', 'd')" />

									<xsl:attribute name="title">
										<xsl:value-of select="format-number((@bid div @rate)-1, '+###.##0%;-#0%', 'd')" />\n(<xsl:value-of select="@bid"/>)
									</xsl:attribute>
									<xsl:value-of select="$mVal" />
								</xsl:if>
							</td>
							<!-- Share Col 6: Tax -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(sum(./*/@tax), '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@tax), '+###.##0,00;-#', 'd')" />
							</td>
							<!-- Share Col 7: Fee -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(sum(./*/@fee), '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@fee), '+###.##0,00;-#', 'd')" />
							</td>
						</tr>

						<!-- SHARE/ISIN DETAILS -->
						<xsl:for-each select="*">
						<xsl:sort select="@date" order="descending" />
							<tr name='details' class='hidden'>
								<xsl:attribute name="data-isin">
									<xsl:value-of select="../@isin" />
								</xsl:attribute>
								<td name="type" align="center">
									<xsl:attribute name="title">
										<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" /> via <xsl:value-of select="@parserType" />
									</xsl:attribute>
									<xsl:if test="../@openamount &lt; 0">
										<input type="checkbox"/>
									</xsl:if>
									<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" /> (<xsl:value-of select="@parserType" />)
								</td>
								<td align="center">
									<xsl:attribute name="title">
										<xsl:value-of select="@date" />
									</xsl:attribute>
									<xsl:value-of select="@date" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@amount, '###.##0,000;#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@amount, '###.##0,000;#', 'd')" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@rate, '###.##0,000', 'd')" />&#160;<xsl:value-of select="@currency" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@rate, '###.##0,000', 'd')" />&#160;<xsl:value-of select="@currency" />
								</td>

								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@total, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@total, '+###.##0,00;-#', 'd')" />
								</td>

								<td align="right">
									<xsl:variable name="wlY" select="format-number(@winloss, '+###.##0,00;-#', 'd')"/>
									<xsl:variable name="wlN">+0,00</xsl:variable>

									<span>
									<xsl:choose>
										<xsl:when test="@winloss and $wlY">
											<xsl:attribute name="title">
												<xsl:value-of select="$wlY" />
											</xsl:attribute>
											<xsl:value-of select="$wlY" />
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="title">
												<xsl:value-of select="$wlN" />
											</xsl:attribute>
											<xsl:value-of select="$wlN" />
										</xsl:otherwise>
									</xsl:choose>
									</span>
								</td>

								<td align="right">
									<xsl:if test="@openAmount and ./../@bid">
										<xsl:attribute name="title">
											<xsl:value-of select="format-number(./../@bid * @openAmount, '+###.##0,000;-#', 'd')" />
										</xsl:attribute>
										<xsl:value-of select="format-number(./../@bid * @openAmount, '+###.##0,000;-#', 'd')" />
									</xsl:if>
								</td>

								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@tax, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@tax, '+###.##0,00;-#', 'd')" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@fee, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@fee, '+###.##0,00;-#', 'd')" />
								</td>
							</tr>
						</xsl:for-each>

					</xsl:for-each>
					</tbody>
				</table>

				<table id="byDividends" class='hidden'>
					<thead>
						<tr><th>Isin</th><th>Name</th><th>Total (no Tax)</th><th>Open Invest</th></tr>
					</thead>
					<tbody>
						<xsl:for-each select="//share/div[starts-with(@date, $year)]/..">
							<xsl:variable name="divInvest" select="@openTotal" />
							<xsl:variable name="divTotal" select="sum(./div[starts-with(@date, $year)]/@winlossNoTax)" />
							<xsl:sort select="sum(./div[starts-with(@date, $year)]/@winlossNoTax)" order="descending" data-type="number" />
<!--							<xsl:sort select="@openTotal" order="descending" data-type="number" />-->
							<tr>
								<td align="center">
									<a>
										<xsl:attribute name="href">#<xsl:value-of select="@isin" /></xsl:attribute>
										<xsl:value-of select="@isin" />
									</a>
								</td>
								<td align="left">
									<xsl:value-of select="@name" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number($divTotal, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number($divTotal, '+###.##0,00;-#', 'd')" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number($divInvest, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number($divInvest, '+###.##0,00;-#', 'd')" />
								</td>
							</tr>
							<xsl:for-each select="./div[starts-with(@date, $year)]">
								<xsl:sort select="@date" order="descending" />
								<tr>
									<td align="right" colspan="2">
										<xsl:attribute name="title">
											<xsl:value-of select="@parserType" />
										</xsl:attribute>
										<xsl:value-of select="@parserType" />
										&#160;
										<xsl:value-of select="substring(@date, 0, 11)" />
										&#160;
									</td>
									<td align="right" title="+">
										<xsl:attribute name="title">
											<xsl:value-of select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')" />
										</xsl:attribute>
										<xsl:value-of select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')" />
									</td>
									<td/>
								</tr>
							</xsl:for-each>
							<tr><td colspan="4" style="background-color: #555555;">&#160;</td></tr>
						</xsl:for-each>
					</tbody>
				</table>

				<table id="byIsinClosed" class='hidden'>
					<thead>
						<tr><th name="type">Type</th><th>Date</th><th>Amount</th><th>Rate</th><th>Total</th><th>Win/Loss</th><th>Tax</th><th>Fee</th>
						</tr>
					</thead>
					<tbody>
					<xsl:for-each select="share[@closed='true']">
						<!-- tr><td class="blank" colspan="8"></td></tr -->
						<tr class="all">
							<xsl:attribute name="disabled">
								<xsl:value-of select="@closed" />
							</xsl:attribute>
							<xsl:attribute name="data-isin">
								<xsl:value-of select="@isin" />
							</xsl:attribute>
							<td colspan="4">
								<xsl:attribute name="title">
									<xsl:value-of select="@name" />&#160;(<xsl:value-of select="@isin" />)
								</xsl:attribute>
								<div><a target="_blank">
									<xsl:attribute name="href">http://www.google.de?q=<xsl:value-of select="@isin" /></xsl:attribute>
									<xsl:attribute name="id"><xsl:value-of select="@isin" /></xsl:attribute>
									<xsl:value-of select="@name" />
									(<xsl:value-of select="@isin" />)
								</a></div>
							</td>
							<td align="right">
								-
							</td>
							<!-- Share Col 5: Win/Loss -->
							<td align="right">
								<xsl:attribute name="title">
									<!-- xsl:value-of select="format-number(@winloss, '+###.##0,00;-#', 'd')" /-->
									<xsl:value-of select="format-number(sum(./*/@winlossNoTax), '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@winlossNoTax), '+###.##0,00;-#', 'd')" />
								<!--xsl:value-of select="format-number(@winloss, '+###.##0,00;-#', 'd')" /-->
							</td>
							<!-- Share Col 6: Tax -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(sum(./*/@tax), '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@tax), '+###.##0,00;-#', 'd')" />
							</td>
							<!-- Share Col 7: Fee -->
							<td align="right">
								<xsl:attribute name="title">
									<xsl:value-of select="format-number(sum(./*/@fee), '+###.##0,00;-#', 'd')" />
								</xsl:attribute>
								<xsl:value-of select="format-number(sum(./*/@fee), '+###.##0,00;-#', 'd')" />
							</td>
						</tr>

						<!-- SHARE/ISIN DETAILS -->
						<xsl:for-each select="*">
						<xsl:sort select="@date" order="descending" />
							<tr name='details' class='hidden'>
								<xsl:attribute name="data-isin">
									<xsl:value-of select="../@isin" />
								</xsl:attribute>
								<td name="type" align="center">
									<xsl:attribute name="title">
										<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" /> via <xsl:value-of select="@parserType" />
									</xsl:attribute>
									<xsl:if test="../@openamount &lt; 0">
										<input type="checkbox"/>
									</xsl:if>
									<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
								</td>
								<td align="center">
									<xsl:attribute name="title">
										<xsl:value-of select="@date" />
									</xsl:attribute>
									<xsl:value-of select="@date" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="@amount" />
									</xsl:attribute>
									<xsl:value-of select="@amount" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@rate, '###.##0,000', 'd')" />&#160;<xsl:value-of select="@currency" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@rate, '###.##0,000', 'd')" />&#160;<xsl:value-of select="@currency" />
								</td>

								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@total, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@total, '+###.##0,00;-#', 'd')" />
								</td>

								<td align="right">
									<xsl:variable name="wlY" select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')"/>
									<xsl:variable name="wlN">+0,00</xsl:variable>

									<span>
									<xsl:choose>
										<xsl:when test="@winloss and $wlY">
											<xsl:attribute name="title">
												<xsl:value-of select="$wlY" />
											</xsl:attribute>
											<xsl:value-of select="$wlY" />
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="title">
												<xsl:value-of select="$wlN" />
											</xsl:attribute>
											<xsl:value-of select="$wlN" />
										</xsl:otherwise>
									</xsl:choose>
									</span>
								</td>

								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@tax, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@tax, '+###.##0,00;-#', 'd')" />
								</td>
								<td align="right">
									<xsl:attribute name="title">
										<xsl:value-of select="format-number(@fee, '+###.##0,00;-#', 'd')" />
									</xsl:attribute>
									<xsl:value-of select="format-number(@fee, '+###.##0,00;-#', 'd')" />
								</td>
							</tr>
						</xsl:for-each>

					</xsl:for-each>
					</tbody>
				</table>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="MonTemplate">
		<xsl:param name="monMin" select="1" />
		<xsl:param name="yearNum" />
		<xsl:if test="12 >= $monMin">
			<fo:block>
				<xsl:variable name="calDate" select="concat($yearNum, '-', format-number($monMin, '00'))" />
				<xsl:variable name="monSum" select="format-number(sum(//share/*[@winlossNoTax][starts-with(@date, $calDate)]/@winlossNoTax), '+###.##0;-#', 'd')" />
				<xsl:variable name="monDivSum" select="format-number(sum(//share/div[@winloss][starts-with(@date, $calDate)]/@winlossNoTax), '+###.##0;-#', 'd')" />
				<td class="center bg30">
					<xsl:attribute name="title">
						monSum: <xsl:value-of select="$monSum" />
						@date: <xsl:value-of select="@date" />
						$calDate: <xsl:value-of select="$calDate" />
					</xsl:attribute>

					<span><xsl:value-of select="$calDate"/></span>
					<br />
					<span>
						<xsl:attribute name="title">
							<xsl:value-of select="$monSum" /> (Win/Loss without Taxes)
						</xsl:attribute>
						<xsl:value-of select="$monSum" />
					</span>
					<br />
					<span>
						<xsl:attribute name="title">
							<xsl:value-of select="$monDivSum" /> (Dividends with Taxes)
						</xsl:attribute>
						<xsl:value-of select="$monDivSum" />
					</span>
				</td>
			</fo:block>

			<xsl:call-template name="MonTemplate">
				<xsl:with-param name="yearNum" select="$yearNum"/>
				<xsl:with-param name="monMin" select="$monMin + 1" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<xsl:template name="CwTemplate">
		<xsl:variable name="cw" select="@calenderWeek" />
		<xsl:param name="calWeek" select="$cw + 1" />

		<xsl:variable name="calYear" select="substring(@date, 1, 4)" />

		<xsl:if test="($calWeek >= $cw - 22 and $calWeek >= 1)">
			<xsl:variable name="calWeekTrans" as="element()*" select="//share/*[@winlossNoTax][@calenderWeek=$calWeek][starts-with(@date, $calYear)]" />
			<xsl:if test="($calWeekTrans)">
				<fo:block>
					<!-- issue if cw-5 = prevYear, maybe use xsl-if and recalculate calyear then? -->
						<xsl:for-each select="$calWeekTrans">
							<xsl:sort select="@date" order="descending" />
							<tr>
								<xsl:attribute name="class">
									<xsl:value-of select="./@parserType" />
									&#160;
									<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
								</xsl:attribute>
								<xsl:apply-templates select="." />
							</tr>
						</xsl:for-each>
						<tr>
							<td colspan="6" class="center bgblack">
								<span>CW: </span>
								<span><xsl:value-of select="$calWeek"/></span>
							</td>
							<td colspan="6" class="center bgblack">
								<span>Total: </span>
								<span>
									<xsl:attribute name="title">
										<xsl:value-of select="(format-number(sum($calWeekTrans/@winlossNoTax), '+###.##0;-#', 'd'))" />
									</xsl:attribute>
									<xsl:value-of select="format-number(sum($calWeekTrans/@winlossNoTax), '+###.##0;-#', 'd')" />
								</span>
							</td>
						</tr>
						<tr class="bgtransparent">
							<td colspan="10" class="noborders">&#160;</td>
						</tr>
				</fo:block>
			</xsl:if>

			<xsl:call-template name="CwTemplate">
				<xsl:with-param name="calWeek" select="$calWeek - 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<xsl:template match="*">
		<!-- xsl:if test="not(position() > 30)"-->
		<td align="center">
			<xsl:attribute name="title">
				<xsl:value-of select="./@date" />
			</xsl:attribute>
			<xsl:value-of select="substring(./@date, 0, 11)" />
		</td>
		<td align="left">
			<span>
				<xsl:attribute name="title">
					<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />
				</xsl:attribute>
				[<xsl:value-of select="translate(name(), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')" />]
			</span>
			<xsl:value-of select="../@name" />
			<br/>
		</td>
		<td align="center">
			<a>
				<xsl:attribute name="title">
					<xsl:value-of select="./@parserType" />
				</xsl:attribute>
				<xsl:attribute name="href">#<xsl:value-of select="../@isin" /></xsl:attribute>
				<xsl:value-of select="../@isin" />
			</a>
		</td>
		<td align="right">
			<xsl:choose>
				<xsl:when test="@amount &lt; 1">
					<xsl:value-of select="format-number(@amount, '###.##0,000;-#', 'd')" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="format-number(@amount, '###.##0;-#', 'd')" />
				</xsl:otherwise>
			</xsl:choose>
		</td>
		<td align="right">
			<xsl:value-of select="format-number(@rate, '###.##0,0000;-#', 'd')" />&#160;<xsl:value-of select="@currency" />
		</td>
		<td align="right">
			<xsl:value-of select="format-number(@total, '###.##0,00;-#', 'd')" />
		</td>
		<td align="right">
			<xsl:attribute name="title">
				<xsl:value-of select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')" />
			</xsl:attribute>
			<xsl:value-of select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')" />
		</td>
		<td align="right">
			<xsl:attribute name="title">
				<xsl:value-of select="format-number(@winlossAvg, '+###.##0,00;-#', 'd')" />
			</xsl:attribute>
			<xsl:value-of select="format-number(@winlossAvg, '+###.##0,00;-#', 'd')" />
		</td>
		<td align="right">
			<xsl:if test="not(contains(name(), 'div')) and not(@total - @winlossNoTax = 0)">
				<xsl:attribute name="title">
					<xsl:value-of select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')" />
				</xsl:attribute>
				<xsl:value-of select="format-number((@winlossNoTax div (@total + 0.001 - @winlossNoTax)), '+##0,00%;-##0,00%', 'd')" />
			</xsl:if>
			<xsl:if test="contains(name(), 'div')">
				<xsl:attribute name="title">
					<xsl:value-of select="format-number(@winlossNoTax, '+###.##0,00;-#', 'd')" />
				</xsl:attribute>
				<xsl:value-of select="format-number((@winlossNoTax div (../@invested) * -1), '+##0,00%;-##0,00%', 'd')" />
			</xsl:if>
		</td>
		<td align="right">
			<xsl:attribute name="title">
				<xsl:value-of select="format-number(@tax, '+###.##0,00;-#', 'd')" />
			</xsl:attribute>
			<xsl:value-of select="format-number(@tax, '+###.##0,00;-#', 'd')" />
		</td>
		<td align="right">
			<xsl:attribute name="title">
				<xsl:value-of select="format-number(@fee, '+###.##0,00;-#', 'd')" />
			</xsl:attribute>
			<xsl:value-of select="format-number(@fee, '+###.##0,00;-#', 'd')" />
		</td>
		<td align="right">
			<xsl:attribute name="title">
				<xsl:value-of select="format-number(@interest, '+###.##0,00;-#', 'd')" />
			</xsl:attribute>
			<xsl:value-of select="format-number(@interest, '+###.##0,00;-#', 'd')" />
		</td>
		<!-- /xsl:if-->
	</xsl:template>
</xsl:stylesheet>
