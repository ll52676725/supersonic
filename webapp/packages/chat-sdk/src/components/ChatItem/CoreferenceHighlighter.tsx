import React, { useMemo } from 'react';
import { Tooltip } from 'antd';
import { CoreferenceItemType } from '../../common/type';

type Props = {
  text: string;
  coreferenceInfo?: CoreferenceItemType[];
  showTooltip?: boolean;
};

const CoreferenceHighlighter: React.FC<Props> = ({ text, coreferenceInfo = [], showTooltip = true }) => {
  const segments = useMemo(() => {
    if (!coreferenceInfo || coreferenceInfo.length === 0) {
      return [{ text, isCoreference: false }];
    }

    const sortedCorefs = [...coreferenceInfo].sort((a, b) => a.startPos - b.startPos);

    const result: Array<{ text: string; isCoreference: boolean; coref?: CoreferenceItemType }> = [];
    let lastEnd = 0;

    for (const coref of sortedCorefs) {
      if (coref.startPos > lastEnd) {
        result.push({
          text: text.substring(lastEnd, coref.startPos),
          isCoreference: false,
        });
      }
      result.push({
        text: text.substring(coref.startPos, coref.endPos),
        isCoreference: true,
        coref,
      });
      lastEnd = coref.endPos;
    }

    if (lastEnd < text.length) {
      result.push({
        text: text.substring(lastEnd),
        isCoreference: false,
      });
    }

    return result;
  }, [text, coreferenceInfo]);

  if (!coreferenceInfo || coreferenceInfo.length === 0) {
    return <span>{text}</span>;
  }

  return (
    <span className="coreference-highlighter">
      {segments.map((segment, index) => {
        if (segment.isCoreference && segment.coref) {
          const tooltipTitle = (
            <div className="coreference-tooltip">
              <div className="tooltip-title">
                <strong>{segment.coref.originalText}</strong>
                <span className="tooltip-arrow">→</span>
                <strong className="tooltip-resolved">{segment.coref.resolvedText}</strong>
              </div>
              <div className="tooltip-source">来源: {segment.coref.sourceTurn}</div>
            </div>
          );

          return (
            <Tooltip key={index} title={showTooltip ? tooltipTitle : ''} placement="top">
              <mark className="coreference-mark">
                {segment.text}
                <sup className="coreference-badge">↑</sup>
              </mark>
            </Tooltip>
          );
        }
        return <span key={index}>{segment.text}</span>;
      })}
    </span>
  );
};

export default CoreferenceHighlighter;
